package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.fileLogger
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewData
import de.drick.compose.hotpreview.plugin.HotPreviewViewModelI
import de.drick.compose.hotpreview.plugin.runCatchingCancellationAware
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

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

@HotPreview(widthDp = 650, heightDp = 1400)
@HotPreview(widthDp = 650, heightDp = 1400, darkMode = false)
@Composable
private fun PreviewMainScreen() {
    val viewModel = remember {
        mockViewModel(getMockData()).apply {
            changeScale(1.0f)
        }
    }
    SelfPreviewBase(viewModel)
}

@Composable
fun MainScreen(model: HotPreviewViewModelI) {
    var previewList: List<HotPreviewData> by remember { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val scale = model.scale
    var compilingInProgress by remember { mutableStateOf(false) }
    var errorMessage: Throwable? by remember { mutableStateOf(null) }

    suspend fun errorHandling(block: suspend () -> Unit) {
        runCatchingCancellationAware {
            block()
            errorMessage = null
        }.onFailure { err ->
            errorMessage = err
            LOG.error(err)
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.Default) {
            compilingInProgress = true
            errorHandling {
                model.executeGradleTask()
                previewList = model.render()
            }
            compilingInProgress = false
        }
    }
    LaunchedEffect(Unit) {
        model.subscribeForFileChanges(scope) {
            refresh()
        }
    }
    LaunchedEffect(Unit) {
        compilingInProgress = true
        errorHandling {
            previewList = model.render()
        }
        compilingInProgress = false
        //refresh()
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
                    scale = scale,
                    onNavigateCode = {
                        LOG.info("Navigate to line: $it")
                        model.navigateCodeLine(it)
                    }
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
                        IconButton(onClick = { model.changeScale(scale + .2f) }) {
                            Icon(AllIconsKeys.General.Add, contentDescription = "ZoomIn")
                        }
                        IconButton(onClick = { model.changeScale(scale - .2f) }) {
                            Icon(AllIconsKeys.General.Remove, contentDescription = "ZoomOut")
                        }
                        IconButton(onClick = { model.changeScale(1f) }) {
                            Icon(AllIconsKeys.General.ActualZoom, contentDescription = "100%")
                        }
                    }
                }
            }
        }
    }
}