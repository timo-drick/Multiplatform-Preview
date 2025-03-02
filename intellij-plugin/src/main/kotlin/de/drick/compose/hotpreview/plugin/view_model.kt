package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import de.drick.compose.hotpreview.plugin.spliteditor.SeamlessEditorWithPreview
import de.drick.compose.hotpreview.plugin.tools.PluginPersistentStore
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettings
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettingsConfigurable
import de.drick.compose.utils.LRUCache
import de.drick.compose.utils.lazySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

data class UIHotPreviewData(
    val functionName: String,
    val lineRange: IntRange?,
    val annotations: List<UIAnnotation>
)

class UIAnnotation(
    val name: String,
    val lineRange: IntRange?
) {
    var state: RenderState by mutableStateOf(NotRenderedYet)
}

interface HotPreviewViewModelI {
    val scale: Float
    val isPureTextEditor: Boolean
    val compilingInProgress: Boolean
    val errorMessage: Throwable?
    val previewList: List<UIHotPreviewData>
    val groups: Set<String>
    val selectedGroup: String?
    fun selectGroup(group: String?)
    fun changeScale(newScale: Float)
    fun navigateCodeLine(line: Int)
    fun monitorChanges(scope: CoroutineScope)
    fun refresh()
    fun openSettings()
}

class HotPreviewViewModel(
    private val project: Project,
    private val previewEditor: HotPreviewView,
    private val file: VirtualFile,
    private val scope: CoroutineScope
): HotPreviewViewModelI {

    private val splitEditor
        get() = requireNotNull(TextEditorWithPreview.getParentSplitEditor(previewEditor) as? SeamlessEditorWithPreview)
    private val textEditor
        get() = splitEditor.textEditor

    val settings = HotPreviewSettings.getInstance().state

    private val classPathService by lazySuspend {
        ClassPathService.getInstance(project, file)
    }

    private val properties = PluginPersistentStore(project, file)

    private val scaleProperty = properties.float("scale", 1f)
    override var scale: Float by mutableStateOf(scaleProperty.get())
        private set

    private val selectGroupProperty = properties.stringNA("selected_group", null)
    override var selectedGroup by mutableStateOf(selectGroupProperty.get())
        private set

    override var groups: Set<String> by mutableStateOf(emptySet())
        private set


    override var isPureTextEditor by mutableStateOf(splitEditor.isPureTextEditor)
    override var compilingInProgress by mutableStateOf(false)
    override var errorMessage: Throwable? by mutableStateOf(null)
    override var previewList: List<UIHotPreviewData> by mutableStateOf(emptyList())

    private var previewFunctions: List<HotPreviewFunction> = emptyList()

    private var compileCounter = 0

    override fun selectGroup(group: String?) {
        selectGroupProperty.set(group)
        val previousSelected = selectedGroup
        selectedGroup = group
        if (previousSelected != group) {
            scope.launch {
                render()
            }
        }
    }

    private fun updateGroups(previewFunctions: List<HotPreviewFunction>) {
        groups = previewFunctions.flatMap { function ->
            function.annotation.groupBy { it.annotation.group }.keys.filter { it.isNotBlank() }
        }.toSet()
        if (groups.contains(selectedGroup).not()) {
            selectGroup(null)
        }
    }

    override fun changeScale(newScale: Float) {
        scaleProperty.set(newScale)
        scale = newScale
    }

    override fun navigateCodeLine(line: Int) {
        val pos = LogicalPosition(line, 0)
        textEditor.editor.apply {
            caretModel.moveToLogicalPosition(pos)
            scrollingModel.scrollTo(pos, ScrollType.MAKE_VISIBLE)
        }
    }

    override fun refresh() {
        scope.launch {
            compilingInProgress = true
            errorHandling {
                val functions = analyzePreviewAnnotations()
                if (functions.isNotEmpty()) {
                    project.useSuspendWorkspace {
                        val module = requireNotNull(getModule(file)) { "No module found for file: ${file.name}" }
                        val desktopModule = getJvmTargetModule(module)
                        val gradleTask = getGradleTaskName(desktopModule)
                        val path = requireNotNull(getModulePath(module)) { "No module path found!" }
                        println("task: $gradleTask path: $path")
                        val parameters = if (settings.gradleParametersEnabled) settings.gradleParameters else ""
                        executeGradleTask(project, gradleTask, parameters, path)
                        compileCounter++
                    }
                }
                previewFunctions = functions
                render()
            }
            compilingInProgress = false
        }
    }

    override fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, HotPreviewSettingsConfigurable::class.java)
    }

    override fun monitorChanges(scope: CoroutineScope) {
        scope.launch {
            compilingInProgress = true
            errorHandling {
                previewFunctions = analyzePreviewAnnotations()
                render()
            }
            compilingInProgress = false
        }
        subscribeForFileChanges(scope) {
            scope.launch {
                if (settings.recompileOnSave) {
                    refresh()
                } else {
                    previewFunctions = analyzePreviewAnnotations()
                    render()
                }
            }
        }
    }

    private fun setPureTextEditorMode(pureTextEditorMode: Boolean) {
        isPureTextEditor = pureTextEditorMode
        splitEditor.isPureTextEditor = isPureTextEditor
    }

    private suspend fun analyzePreviewAnnotations(): List<HotPreviewFunction> {
        val previewFunctions = findPreviewAnnotations(project, file)
        setPureTextEditorMode(previewFunctions.isEmpty())
        updateGroups(previewFunctions)
        return previewFunctions
    }

    private suspend fun errorHandling(block: suspend () -> Unit) {
        runCatchingCancellationAware {
            block()
            errorMessage = null
        }.onFailure { err ->
            errorMessage = err
            LOG.warn(err)
        }
    }


    data class RenderQueue(
        val function: HotPreviewFunction,
        val annotation: HotPreviewModel,
        val ui: UIAnnotation
    )
    data class RenderCacheKey(
        val name: String,
        val annotation: HotPreviewModel
    )

    private val renderCache = LRUCache<RenderCacheKey, RenderedImage>(20)

    suspend fun render() {
        val renderList = mutableListOf<RenderQueue>()

        previewList = previewFunctions.map { function ->
            val annotations = function.annotation.filter {
                selectedGroup == null || it.annotation.group == selectedGroup
            }.map {
                UIAnnotation(
                    it.annotation.name,
                    it.lineRange
                ).also { ui ->
                    renderCache[RenderCacheKey(function.name, it.annotation)]?.let {  image ->
                        ui.state = image
                    }
                    renderList.add(RenderQueue(function, it.annotation, ui))
                }
            }
            UIHotPreviewData(
                functionName = function.name,
                lineRange = function.lineRange,
                annotations = annotations
            )
        }

        withContext(Dispatchers.Default) {
            val (renderClassLoader, duration) = measureTimedValue {
                classPathService.get().getRenderClassLoaderInstance(compileCounter)
            }
            println("Get classloader: $duration")
            renderList.forEach { queue ->
                // Workaround for legacy resource loading in old compose code
                // See androidx.compose.ui.res.ClassLoaderResourceLoader
                // It uses the contextClassLoader to load the resources.
                val previousContextClassLoader = Thread.currentThread().contextClassLoader
                // For new compose.components resource system a LocalCompositionProvider is used.
                Thread.currentThread().contextClassLoader = renderClassLoader.classLoader
                val state = try {
                    renderPreview(renderClassLoader, queue.function, queue.annotation)
                } finally {
                    Thread.currentThread().contextClassLoader = previousContextClassLoader
                }
                //val state = render(renderClassLoader, queue.function, queue.annotation)
                queue.ui.state = state
                if (state is RenderedImage) renderCache[RenderCacheKey(queue.function.name, queue.annotation)] = state
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
