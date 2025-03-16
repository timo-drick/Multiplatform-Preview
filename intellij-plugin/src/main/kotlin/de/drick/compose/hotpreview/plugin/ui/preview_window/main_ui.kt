package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.editorTabStyle
import org.jetbrains.jewel.ui.theme.groupHeaderStyle

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

@OptIn(ExperimentalResourceApi::class)
@HotPreview(widthDp = 650, heightDp = 1400)
@HotPreview(widthDp = 650, heightDp = 1400, darkMode = false)
@Composable
private fun PreviewMainScreen() {
    val env = rememberResourceEnvironment()
    val viewModel = remember {
        mockViewModel(env, getMockData()).apply {
            changeScale(1f)
        }
    }
    SelfPreviewBase(viewModel)
}

@Composable
fun MainScreen(model: HotPreviewViewModelI) {
    val scope = rememberCoroutineScope()

    val previewList = model.previewList
    val scale = model.scale
    val compilingInProgress = model.compilingInProgress
    val error: Throwable? = model.errorMessage

    LaunchedEffect(Unit) {
        model.monitorChanges(scope)
    }

    Column(Modifier.fillMaxSize().background(JewelTheme.editorTabStyle.colors.background)) {
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = compilingInProgress,
            groups = if (model.selectedTab == null) model.groups else emptySet(),
            selectedGroup = model.selectedGroup,
            onAction = { action ->
                when (action) {
                    TopBarAction.Refresh -> model.refresh()
                    TopBarAction.OpenSettings -> model.openSettings()
                    is TopBarAction.SelectGroup -> model.selectGroup(action.group)
                    TopBarAction.ToggleLayout -> model.toggleLayout()
                }
            }
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
                        scale = scale,
                        selectedGroup = model.selectedGroup,
                        onNavigateCode = { model.navigateCodeLine(it) },
                        requestPreviews = { model.requestPreviews(it) }
                    )
                } else {
                    if (previewList.isEmpty()) {
                        Text("No previews detected yet.")
                    } else {
                        PreviewGalleryPanel(
                            modifier = Modifier.fillMaxWidth(),
                            hotPreviewList = previewList,
                            selectedTab = selectedTab,
                            scale = scale,
                            requestPreviews = { model.requestPreviews(it) },
                            onNavigateCode = { model.navigateCodeLine(it) },
                            onSelectTab = { model.selectTab(it) }
                        )
                    }
                }
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