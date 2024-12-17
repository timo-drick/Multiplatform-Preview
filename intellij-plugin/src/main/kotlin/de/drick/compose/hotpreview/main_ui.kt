package de.drick.compose.hotpreview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import de.drick.compose.hotpreview.livecompile.hotRecompileFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.io.File


@Composable
fun MainScreen(project: Project, file: VirtualFile) {
    var previewList: List<HotPreviewData> by remember { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val projectAnalyzer = remember {
        ProjectAnalyzer(project)
    }
    suspend fun render() {
        val fileClass = projectAnalyzer.loadFileClass(file)
        previewList = renderPreviewForClass(fileClass)
    }
    fun refresh() {
        scope.launch(Dispatchers.Default) {
            runCatchingCancellationAware {
                projectAnalyzer.executeGradleTask(file)
                render()
            }.onFailure { err ->
                err.printStackTrace()
            }
        }
    }
    LaunchedEffect(Unit) {
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    println("File event: ${events.joinToString { it.toString() }}")
                }
            }
        )
    }
    LaunchedEffect(Unit) {
        FileDocumentManager.getInstance().getDocument(file)?.let { document ->
            document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    println("Document event: $event")
                }
            })
        }
    }
    LaunchedEffect(Unit) {
        runCatchingCancellationAware {
            render()

            val info = projectAnalyzer.getSdkInfo()
            val outputFolder = projectAnalyzer.getOutputFolder(file)
            val sourceSet = projectAnalyzer.getSourceFolder(file)
            val classPath = projectAnalyzer.getJvmClassPath(file)
            val jdkHome = projectAnalyzer.getSdkInfo()

            hotRecompileFlow(
                compile = {
                    projectAnalyzer.executeGradleTask(file)
                },
                targetJvmVersion = "17",
                classPath = classPath.toList(),
                sourceSet = sourceSet,
                outputFolder = File(outputFolder),
                jdkHome = jdkHome
            ).collect {
                render()
            }
            //refresh()
        }.onFailure { err ->
            err.printStackTrace()
        }
    }

    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = { refresh() }) {
            Icon(AllIconsKeys.General.Refresh, contentDescription = "Refresh")
        }
        PreviewGridPanel(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            hotPreviewList = previewList
        )
    }
}