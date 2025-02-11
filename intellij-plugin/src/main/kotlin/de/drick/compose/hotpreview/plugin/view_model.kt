package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.application.readActionBlocking
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import de.drick.compose.hotpreview.plugin.spliteditor.SeamlessEditorWithPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLClassLoader

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

interface HotPreviewViewModelI {
    val scale: Float
    val isPureTextEditor: Boolean
    val compilingInProgress: Boolean
    val errorMessage: Throwable?
    var previewList: List<HotPreviewData>
    fun changeScale(newScale: Float)
    fun navigateCodeLine(line: Int)
    fun monitorChanges(scope: CoroutineScope)
    suspend fun refresh()
    //suspend fun render(): List<HotPreviewData>
    //suspend fun executeGradleTask()
    //fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit)
}

class HotPreviewViewModel(
    private val project: Project,
    private val previewEditor: HotPreviewView,
    private val file: VirtualFile
): HotPreviewViewModelI {
    private val splitEditor = requireNotNull(TextEditorWithPreview.getParentSplitEditor(previewEditor) as? SeamlessEditorWithPreview)
    private val textEditor = splitEditor.textEditor
    private val projectAnalyzer = ProjectAnalyzer(project)
    private val workspaceAnalyzer = WorkspaceAnalyzer(project)

    private val properties = PluginPersistentStore(project, file)
    private val scaleProperty = properties.float("scale", 1f)

    init {
        //val previewFunctions = projectAnalyzer.findPreviewAnnotations(file)
        //splitEditor.setVerticalSplit(false)
        /*EventQueue.invokeLater {
            splitEditor.setVerticalSplit(true)
        }*/
    }

    override var scale: Float by mutableStateOf(scaleProperty.get())
        private set


    override var isPureTextEditor by mutableStateOf(splitEditor.isPureTextEditor)
    override var compilingInProgress by mutableStateOf(false)
    override var errorMessage: Throwable? by mutableStateOf(null)
    override var previewList: List<HotPreviewData> by mutableStateOf(emptyList())

    override fun changeScale(newScale: Float) {
        scaleProperty.set(newScale)
        scale = newScale
    }

    override fun navigateCodeLine(line: Int) {
        val pos = LogicalPosition(line, 0)
        textEditor.editor.caretModel.moveToLogicalPosition(pos)
        textEditor.editor.scrollingModel.scrollTo(pos, ScrollType.MAKE_VISIBLE)
    }

    override suspend fun refresh() {
        compilingInProgress = true
        errorHandling {
            val previewFunctions = analyzePreviewAnnotations()
            if (previewFunctions.isNotEmpty()) {
                projectAnalyzer.executeGradleTask(file)
            }
            previewList = render(previewFunctions)
        }
        compilingInProgress = false
    }

    override fun monitorChanges(scope: CoroutineScope) {
        scope.launch {
            compilingInProgress = true
            errorHandling {
                val previewFunctions = analyzePreviewAnnotations()
                previewList = render(previewFunctions)
            }
            compilingInProgress = false
        }
        subscribeForFileChanges(scope) {
            scope.launch {
                refresh()
            }
        }
    }

    private fun setPureTextEditorMode(pureTextEditorMode: Boolean) {
        isPureTextEditor = pureTextEditorMode
        splitEditor.isPureTextEditor = isPureTextEditor
    }

    private suspend fun analyzePreviewAnnotations(): List<HotPreviewFunction> {
        val previewFunctions = projectAnalyzer.findPreviewAnnotations(file)
        setPureTextEditorMode(previewFunctions.isEmpty())
        return previewFunctions
    }

    private suspend fun errorHandling(block: suspend () -> Unit) {
        runCatchingCancellationAware {
            block()
            errorMessage = null
        }.onFailure { err ->
            errorMessage = err
            LOG.error(err)
        }
    }

    private suspend fun render(previewFunctions: List<HotPreviewFunction>): List<HotPreviewData> {
        if (previewFunctions.isEmpty()) return emptyList()
        /*val test = workspaceAnalyzer.getClassPathForFile(file)
        test.forEach {
            println(it)
        }*/
        //val renderService = project.service<RenderService>()
        val dumbService = project.service<DumbService>()
        LOG.debug("is in dumb mode: ${dumbService.isDumb}")
        println("is in dumb mode: ${dumbService.isDumb}")

        val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
        LOG.debug(skikoLibs.toString())
        val classPath = projectAnalyzer.getClassPath(file) + skikoLibs

        val classLoader = URLClassLoader(
            classPath.toTypedArray(),
            null
        )
        val fileClassName = kotlinFileClassName(file)

        return readActionBlocking {
            // Workaround for legacy resource loading in old compose code
            // See androidx.compose.ui.res.ClassLoaderResourceLoader
            // It uses the contextClassLoader to load the resources.
            val previousContextClassLoader = Thread.currentThread().contextClassLoader
            // For new compose.components resource system a LocalCompositionProvider is used.
            Thread.currentThread().contextClassLoader = classLoader
            try {
                renderPreview(classLoader, fileClassName, previewFunctions)
            } finally {
                Thread.currentThread().contextClassLoader = previousContextClassLoader
            }
        }
    }


    private fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit) {
        project.messageBus.connect(scope).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    // Check file is part of the current module
                    //TODO check for source folders
                    var changedKotlinFile = false
                    events.forEach { event ->
                        event.file?.let { file ->
                            if (file.extension == "kt") {
                                LOG.debug("File event: $event")
                                changedKotlinFile = true
                            }
                        }
                    }
                    if (changedKotlinFile) onChanged()
                }
            }
        )
    }

    //TODO
    private fun subscribeForFileEditing() {
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                LOG.debug("Document event: $event")
            }
        })
    }
}
