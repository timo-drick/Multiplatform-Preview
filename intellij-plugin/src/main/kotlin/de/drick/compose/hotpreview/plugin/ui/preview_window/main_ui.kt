package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.fileLogger
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.theme.editorTabStyle
import org.jetbrains.jewel.ui.theme.groupHeaderStyle

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

@OptIn(ExperimentalResourceApi::class)
@HotPreview(widthDp = 1280, heightDp = 800, density = 1.5f)
@HotPreview(widthDp = 1280, heightDp = 800, darkMode = false, density = 1.5f)
@Composable
private fun PreviewMainScreen() {
    val env = rememberResourceEnvironment()
    val viewModel = remember {
        mockViewModel(env, getMockData()).apply {
            scaleState.setNeutral()
        }
    }
    SelfPreviewBase(viewModel)
}

@Composable
fun MainScreen(model: HotPreviewViewModelI) {
    val scope = rememberCoroutineScope()

    val previewList = model.previewList
    val scaleState = model.scaleState
    val compilingInProgress = model.compilingInProgress
    val error: Throwable? = model.errorMessage

    LaunchedEffect(Unit) {
        model.onAction(HotPreviewAction.MonitorChanges(scope))
    }

    Column(Modifier.fillMaxSize().background(JewelTheme.editorTabStyle.colors.background)) {
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = compilingInProgress,
            groups = if (model.selectedTab == null) model.groups else emptySet(),
            selectedGroup = model.selectedGroup,
            isOutdatedHotPreviewAnnotation = model.outdatedAnnotationVersion,
            onAction = model::onAction
        )
        val style = JewelTheme.groupHeaderStyle
        Divider(
            modifier = Modifier.fillMaxWidth(),
            orientation = Orientation.Horizontal,
            color = style.colors.divider,
            thickness = style.metrics.dividerThickness,
        )
        if (error != null) {
            ErrorPanel(
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                error = error
            )
        } else {
            var showZoomControls by remember { mutableStateOf(false) }
            val selectedTab = model.selectedTab
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().onHover { showZoomControls = it }) {
                if (selectedTab == null) {
                    PreviewGridPanel(
                        modifier = Modifier.fillMaxWidth(),
                        hotPreviewList = previewList,
                        scaleState = scaleState,
                        selectedGroup = model.selectedGroup,
                        onNavigateCode = { model.onAction(HotPreviewAction.NavigateCodeLine(it)) },
                        requestPreviews = model::requestPreviews,
                        onGetGutterIconViewModel = { model.getGutterIconViewModel(it) }
                    )
                } else {
                    if (previewList.isEmpty()) {
                        Text("No previews detected yet.")
                    } else {
                        PreviewGalleryPanel(
                            modifier = Modifier.fillMaxWidth(),
                            hotPreviewList = previewList,
                            selectedTab = selectedTab,
                            scaleState = scaleState,
                            requestPreviews = { model.requestPreviews(it) },
                            onNavigateCode = { model.onAction(HotPreviewAction.NavigateCodeLine(it)) },
                            onSelectTab = { model.onAction(HotPreviewAction.SelectTab(it)) },
                            onGetGutterIconViewModel = { model.getGutterIconViewModel(it) }
                        )
                    }
                }
                ZoomControls(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    visible = showZoomControls,
                    scaleState = scaleState
                )
            }
        }
    }
}

@Composable
fun ErrorPanel(
    error: Throwable,
    modifier: Modifier = Modifier
) {
    val stackTrace = remember(error) {
        error.stackTraceToString().replace("\t", "    ")
    }
    Box(modifier) {
        VerticallyScrollableContainer {
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