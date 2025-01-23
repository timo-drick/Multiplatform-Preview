package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.HorizontallyScrollableContainer
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.editorTabStyle


@Composable
fun MainScreen(project: Project, file: VirtualFile) {
    var previewList: List<HotPreviewData> by remember { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val projectAnalyzer = remember {
        ProjectAnalyzer(project)
    }
    var scale by remember { mutableStateOf(1f) }
    var compilingInProgress by remember { mutableStateOf(false) }
    var errorMessage: Throwable? by remember { mutableStateOf(null) }

    suspend fun errorHandling(block: suspend () -> Unit) {
        runCatchingCancellationAware {
            block()
            errorMessage = null
        }.onFailure { err ->
            errorMessage = err
            err.printStackTrace()
        }
    }

    suspend fun render() {
        val fileClass = projectAnalyzer.loadFileClass(file)
        // Workaround for legacy resource loading in old compose code
        // See androidx.compose.ui.res.ClassLoaderResourceLoader
        // It uses the contextClassLoader to load the resources.
        val previousContextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = fileClass.classLoader
        val previewFunctions = projectAnalyzer.findPreviewAnnotations(file)
        // For new compose.components resource system a LocalCompositionProvider is used.
        previewList = renderPreview(fileClass, previewFunctions)
        Thread.currentThread().contextClassLoader = previousContextClassLoader
    }
    fun refresh() {
        scope.launch(Dispatchers.Default) {
            compilingInProgress = true
            errorHandling {
                projectAnalyzer.executeGradleTask(file)
                render()
            }
            compilingInProgress = false
        }
    }
    LaunchedEffect(Unit) {
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
                    if (changedKotlinFile) refresh()
                }
            }
        )
    }
    LaunchedEffect(Unit) {
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                println("Document event: $event")
            }
        })
    }
    LaunchedEffect(Unit) {
        errorHandling {
            render()
        }
        refresh()
        /*runCatchingCancellationAware {
            render()

            /*val info = projectAnalyzer.getSdkInfo()
            val outputFolder = projectAnalyzer.getOutputFolder(file)
            val sourceSet = projectAnalyzer.getSourceFolder(file)
            val classPath = projectAnalyzer.getJvmClassPath(file)
            val jdkHome = projectAnalyzer.getSdkInfo()
            val module = projectAnalyzer.getModule(file)
            requireNotNull(module)
            val jvmModule = projectAnalyzer.getJvmTargetModule(module)
            println("$jvmModule")

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
            }*/
            //refresh()
        }.onFailure { err ->
            err.printStackTrace()
        }*/
    }

    Column(Modifier.fillMaxSize().background(JewelTheme.editorTabStyle.colors.background)) {
        Row(
            modifier = Modifier
                .background(JewelTheme.globalColors.panelBackground)
                .fillMaxWidth()
                .align(Alignment.End)
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { refresh() },
                enabled = compilingInProgress.not()
            ) {
                if (compilingInProgress) {
                    CircularProgressIndicator()
                } else {
                    Icon(AllIconsKeys.General.Refresh, contentDescription = "Refresh")
                }
            }
        }
        if (errorMessage != null) {
            errorMessage?.let { error ->
                val stackTrace = remember(error) {
                    error.stackTraceToString().replace("\t", "    ")
                }
                Box(Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                ) {
                    VerticallyScrollableContainer {
                        HorizontallyScrollableContainer {
                            Column {
                                Text(
                                    text = error.message ?: "",
                                    color = JewelTheme.globalColors.text.error,
                                    style = JewelTheme.editorTextStyle
                                )
                                Text(
                                    text = stackTrace,
                                    color = JewelTheme.globalColors.text.error,
                                    style = JewelTheme.editorTextStyle
                                )
                            }
                        }
                    }
                }
            }
        } else {
            var showZoomControls by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().onHover { showZoomControls = it }) {
                PreviewGridPanel(
                    modifier = Modifier.fillMaxWidth(),
                    hotPreviewList = previewList,
                    scale = scale
                )
                if (showZoomControls) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { scale += .2f }) {
                            Icon(AllIconsKeys.General.Add, contentDescription = "ZoomIn")
                        }
                        IconButton(onClick = { scale -= .2f }) {
                            Icon(AllIconsKeys.General.Remove, contentDescription = "ZoomOut")
                        }
                        IconButton(onClick = { scale = 1f }) {
                            Icon(AllIconsKeys.General.ActualZoom, contentDescription = "100%")
                        }
                    }
                }
            }
        }
    }
}