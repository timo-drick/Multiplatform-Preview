package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewViewModel.RenderCacheKey
import de.drick.compose.hotpreview.plugin.UIFunctionAnnotation
import de.drick.compose.hotpreview.plugin.UIHotPreviewData
import de.drick.compose.hotpreview.plugin.UIRenderState
import de.drick.compose.hotpreview.plugin.ui.components.TabBar
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.groupHeaderStyle


@OptIn(ExperimentalResourceApi::class)
@HotPreview(widthDp = 500, heightDp = 500)
@HotPreview(widthDp = 650, heightDp = 1400, darkMode = false)
@Composable
private fun PreviewPreviewGalleryPanel() {
    val env = rememberResourceEnvironment()
    val data = remember {
        getMockData()
    }
    SelfPreviewTheme {
        PreviewGalleryPanel(
            hotPreviewList = data,
            scale = 1f,
            selectedTab = 0,
            requestPreviews = { resolveRenderState(env, it) },
            onNavigateCode = {},
            onSelectTab = {}
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewGalleryPanel(
    hotPreviewList: List<UIHotPreviewData>,
    scale: Float,
    selectedTab: Int,
    modifier: Modifier = Modifier,
    requestPreviews: (Set<RenderCacheKey>) -> Map<RenderCacheKey, UIRenderState>,
    onNavigateCode: (line: Int) -> Unit,
    onSelectTab: (Int) -> Unit
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
    Column {
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
        VerticallyScrollableContainer(
            modifier = modifier.fillMaxSize(),

        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PreviewItem(
                    modifier = Modifier.clickable(
                        onClick = {
                            onNavigateCode(selectedItem.annotation.lineRange.start)
                        },
                        interactionSource = null,
                        indication = null
                    ),
                    name = selectedItem.toName(),
                    renderState = uiRenderState.state,
                    scale = scale,
                    hasFocus = false
                )
            }
        }
    }

}