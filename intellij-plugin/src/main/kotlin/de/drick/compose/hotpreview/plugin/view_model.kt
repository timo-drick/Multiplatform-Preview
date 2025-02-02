package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLClassLoader

interface HotPreviewViewModelI {
    val scale: Float
    fun changeScale(newScale: Float)
    fun navigateCodeLine(line: Int)
    suspend fun render(): List<HotPreviewData>
    suspend fun executeGradleTask()
    fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit)
}

class HotPreviewViewModel(
    private val project: Project,
    private val textEditor: TextEditor,
    private val file: VirtualFile
): HotPreviewViewModelI {

    private val projectAnalyzer = ProjectAnalyzer(project)
    private val workspaceAnalyzer = WorkspaceAnalyzer(project)

    private val properties = PluginPersistentStore(project, file)
    private val scaleProperty = properties.float("scale", 1f)

    override var scale: Float by mutableStateOf(scaleProperty.get())
        private set

    override fun changeScale(newScale: Float) {
        scaleProperty.set(newScale)
        scale = newScale
    }

    override fun navigateCodeLine(line: Int) {
        val pos = LogicalPosition(line, 0)
        textEditor.editor.caretModel.moveToLogicalPosition(pos)
        textEditor.editor.scrollingModel.scrollTo(pos, ScrollType.MAKE_VISIBLE)
    }

    override suspend fun render(): List<HotPreviewData> {
        /*val test = workspaceAnalyzer.getClassPathForFile(file)
        test.forEach {
            println(it)
        }*/


        val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
        println(skikoLibs)
        val classPath = projectAnalyzer.getClassPath(file) + skikoLibs
        //Add skiko libs

        val classLoader = URLClassLoader(
            classPath.toTypedArray(),
            null
        )
        val fileClassName = kotlinFileClassName(file)
        val previewFunctions = projectAnalyzer.findPreviewAnnotations(file)

        // Workaround for legacy resource loading in old compose code
        // See androidx.compose.ui.res.ClassLoaderResourceLoader
        // It uses the contextClassLoader to load the resources.
        val previousContextClassLoader = Thread.currentThread().contextClassLoader
        // For new compose.components resource system a LocalCompositionProvider is used.
        Thread.currentThread().contextClassLoader = classLoader
        return try {
            renderPreview(classLoader, fileClassName, previewFunctions)
        } finally {
            Thread.currentThread().contextClassLoader = previousContextClassLoader
        }
    }

    override suspend fun executeGradleTask() {
        projectAnalyzer.executeGradleTask(file)
    }

    override fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit) {
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
                                println("File event: $event")
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
    fun subscribeForFileEditing() {
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                println("Document event: $event")
            }
        })
    }
}

object RuntimeLibrariesManager {
    private var tmpFolder: File? = null

    private val runtimeLibs = listOf(
        "hot_preview_render-all.jar"
    )

    private fun getResUrl(name: String) = this.javaClass.classLoader.getResource(name)

    private suspend fun initialize(): File = withContext(Dispatchers.IO) {
        val dir = tmpFolder
        if (dir == null) {
            val dir = FileUtil.createTempDirectory("hotpreview", "libs", true)
            tmpFolder = dir
            //Copy libraries
            println("Temp dir: $dir")
            runtimeLibs.forEach { fileName ->
                val url = getResUrl(fileName)
                val outputFile = File(dir, fileName)
                url.openStream().use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            dir
        } else {
            dir
        }
    }

    suspend fun getRuntimeLibs(): List<URL> {
        val tmpFolder = initialize()
        return runtimeLibs.map {
            File(tmpFolder, it).toURI().toURL()
        }
    }
}