package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.service.RenderCacheKey
import de.drick.compose.hotpreview.plugin.tools.MockPersistentStore
import de.drick.compose.hotpreview.plugin.ui.components.TabBar
import de.drick.compose.hotpreview.plugin.ui.components.ScrollableContainer
import de.drick.compose.hotpreview.plugin.ui.guttericon.DialogGutterIconSettings
import de.drick.compose.hotpreview.plugin.ui.guttericon.GutterIconViewModelI
import de.drick.compose.hotpreview.plugin.ui.guttericon.mockGutterIconViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.groupHeaderStyle


@OptIn(ExperimentalResourceApi::class)
@HotPreview(name = "dark mode", widthDp = 500, heightDp = 500)
@HotPreview(name = "light mode", widthDp = 500, heightDp = 500, darkMode = false)
@Composable
private fun PreviewPreviewGalleryPanel() {
    val env = rememberResourceEnvironment()
    val data = remember {
        getMockData()
    }
    val scaleState = remember {
        ScaleState(MockPersistentStore())
    }
    SelfPreviewTheme {
        PreviewGalleryPanel(
            hotPreviewList = data,
            scaleState = scaleState,
            selectedTab = 0,
            requestPreviews = { resolveRenderState(env, it) },
            onNavigateCode = {},
            onSelectTab = {},
            onGetGutterIconViewModel = { mockGutterIconViewModel }
        )
    }
}

fun UIFunctionAnnotation.toName(): String {
    val fName = functionName
    val aName = annotation.name
    return when {
        aName.isNotBlank() -> "$fName - $aName"
        else -> fName
    }
}

fun fitScalingToContent(
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
    contentSize: IntSize,
    scaleState: ScaleState
) {
    val viewportIntSize = IntSize(horizontalScrollState.viewportSize, verticalScrollState.viewportSize)
    if (scaleState.fitToContent && scaleState.minReached().not() &&
        (contentSize.width > viewportIntSize.width || contentSize.height > viewportIntSize.height)
    ) {
        scaleState.fitOut()
    }
    //Check if we can increase size by 15%
    if (scaleState.fitToContent && scaleState.maxReached().not() &&
        (contentSize.width * 1.15f < viewportIntSize.width && contentSize.height * 1.15f < viewportIntSize.height)
    ) {
        scaleState.fitIn()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewGalleryPanel(
    hotPreviewList: List<UIHotPreviewData>,
    scaleState: ScaleState,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    requestPreviews: (Set<RenderCacheKey>) -> Map<RenderCacheKey, UIRenderState>,
    onNavigateCode: (line: Int) -> Unit,
    onSelectTab: (Int) -> Unit,
    onGetGutterIconViewModel: (UIAnnotation) -> GutterIconViewModelI
) {
    val flatPreviewList = remember(hotPreviewList) {
        require(hotPreviewList.isNotEmpty()) { "Preview list must not be empty!" }
        hotPreviewList.flatMap { data -> data.annotations.map { UIFunctionAnnotation(data.functionName, it) } }
    }
    val selectedItem = remember(hotPreviewList, selectedTab) {
        if (selectedTab >= flatPreviewList.size) flatPreviewList.last()
        else flatPreviewList.getOrElse(selectedTab) {
            flatPreviewList.first()
        }
    }
    val uiRenderState = remember(hotPreviewList, selectedTab) {
        val key = selectedItem.annotation.renderCacheKey
        val map = requestPreviews(setOf(key))
        requireNotNull(map[key]) { "Returned map does not contain the requested key!" }
    }
    val tabs = remember(hotPreviewList, selectedItem) {
        flatPreviewList.map { it.toName() }
    }
    var gutterIconViewModel: GutterIconViewModelI? by remember { mutableStateOf(null) }

    Column(modifier) {
        Spacer(Modifier.height(4.dp))
        TabBar(
            selectedTab = selectedTab,
            tabNames = tabs,
            onSelectTab = { onSelectTab(it) }
        )
        Spacer(Modifier.height(4.dp))
        val style = JewelTheme.groupHeaderStyle
        Divider(
            modifier = Modifier.fillMaxWidth(),
            orientation = Orientation.Horizontal,
            color = style.colors.divider,
            thickness = style.metrics.dividerThickness,
        )
        Spacer(Modifier.height(4.dp))
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()
        var contentSize by remember { mutableStateOf(IntSize.Zero) }
        fitScalingToContent(
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,
            contentSize = contentSize,
            scaleState = scaleState,
        )
        ScrollableContainer(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalScrollState = verticalScrollState,
            horizontalScrollState = horizontalScrollState,
        ) {
            PreviewItem(
                modifier = Modifier
                    .onSizeChanged { contentSize = it }
                    .align(Alignment.Center)
                    .clickable(
                        onClick = {
                            onNavigateCode(selectedItem.annotation.lineRange.first)
                        },
                    interactionSource = null,
                    indication = null
                ),
                name = selectedItem.toName(),
                renderState = uiRenderState.state,
                scale = scaleState.scale,
                hasFocus = false,
                onSettings = if (selectedItem.annotation.isAnnotationClass.not()) {
                    { gutterIconViewModel = onGetGutterIconViewModel(selectedItem.annotation) }
                } else null
            )
        }
        DialogGutterIconSettings(
            viewModel = gutterIconViewModel,
            onClose = { gutterIconViewModel = null }
        )
    }
}