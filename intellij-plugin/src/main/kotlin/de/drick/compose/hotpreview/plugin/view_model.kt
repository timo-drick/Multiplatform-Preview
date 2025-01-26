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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.CoroutineScope

class HotPreviewViewModel(
    private val project: Project,
    private val textEditor: TextEditor,
    private val file: VirtualFile
) {

    private val projectAnalyzer = ProjectAnalyzer(project)

    private val properties = PluginPersistentStore(project, file)
    private val scaleProperty = properties.float("scale", 1f)

    var scale: Float by mutableStateOf(scaleProperty.get())
        private set

    fun changeScale(newScale: Float) {
        scaleProperty.set(newScale)
        scale = newScale
    }

    fun navigateCodeLine(line: Int) {
        val pos = LogicalPosition(line, 0)
        textEditor.editor.caretModel.moveToLogicalPosition(pos)
        textEditor.editor.scrollingModel.scrollTo(pos, ScrollType.MAKE_VISIBLE)
    }

    suspend fun render(): List<HotPreviewData> {
        val fileClass = projectAnalyzer.loadFileClass(file)
        // Workaround for legacy resource loading in old compose code
        // See androidx.compose.ui.res.ClassLoaderResourceLoader
        // It uses the contextClassLoader to load the resources.
        val previousContextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = fileClass.classLoader
        val previewFunctions = projectAnalyzer.findPreviewAnnotations(file)
        // For new compose.components resource system a LocalCompositionProvider is used.
        val previewList = renderPreview(fileClass, previewFunctions)
        Thread.currentThread().contextClassLoader = previousContextClassLoader
        return previewList
    }

    suspend fun executeGradleTask() {
        projectAnalyzer.executeGradleTask(file)
    }

    fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit) {
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
