package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewViewModel.RenderCacheKey
import de.drick.compose.hotpreview.plugin.UIHotPreviewData
import de.drick.compose.hotpreview.plugin.UIRenderState
import de.drick.compose.hotpreview.plugin.ui.components.FoldableSection
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.*


@OptIn(ExperimentalResourceApi::class)
@HotPreview(widthDp = 900, heightDp = 1400)
@HotPreview(widthDp = 650, heightDp = 1400, darkMode = false)
@Composable
private fun PreviewPreviewGridPanel() {
    val env = rememberResourceEnvironment()
    val data = remember {
        getMockData()
    }
    SelfPreviewTheme {
        PreviewGridPanel(
            hotPreviewList = data,
            scale = 1f,
            selectedGroup = "dark",
            requestPreviews = { resolveRenderState(env, it) },
            onNavigateCode = {}
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewGridPanel(
    hotPreviewList: List<UIHotPreviewData>,
    scale: Float,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
    onNavigateCode: (Int) -> Unit,
    requestPreviews: (Set<RenderCacheKey>) -> Map<RenderCacheKey, UIRenderState>
) {
    val previewList = remember(hotPreviewList, selectedGroup) {
        hotPreviewList.map {
            it.copy(
                annotations = it.annotations.filter {
                    selectedGroup == null || it.renderCacheKey.annotation.group == selectedGroup
                }
            )
        }
    }
    val previewMap = remember(previewList) {
        val renderCacheKeys = previewList.flatMap { function ->
            function.annotations.map { annotation ->
                annotation.renderCacheKey
            }
        }.toSet()
        requestPreviews(renderCacheKeys)
    }
    VerticallyScrollableContainer(
        modifier = modifier.fillMaxSize()
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewList.forEach { preview ->
                if (preview.annotations.size > 1) {
                    FoldableSection(preview.functionName) {
                        PreviewSection(
                            hasHeader = true,
                            scale = scale,
                            preview = preview,
                            renderStateMap = previewMap,
                            onNavigateCode = onNavigateCode
                        )
                    }
                } else {
                    PreviewSection(
                        hasHeader = false,
                        scale = scale,
                        preview = preview,
                        renderStateMap = previewMap,
                        onNavigateCode = onNavigateCode
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewSection(
    hasHeader: Boolean,
    scale: Float,
    preview: UIHotPreviewData,
    renderStateMap: Map<RenderCacheKey, UIRenderState>,
    onNavigateCode: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        preview.annotations.forEachIndexed { index, annotation ->
            var isFocused by remember { mutableStateOf(false) }

            val fName = preview.functionName
            val aName = annotation.name
            val name = when {
                hasHeader && aName.isNotBlank() -> aName
                aName.isNotBlank() -> "$fName - $aName"
                else -> fName
            }
            val uiRenderState = renderStateMap[annotation.renderCacheKey] ?: UIRenderState()
            PreviewItem(
                modifier = Modifier.onHover { isFocused = it }.clickable(
                    onClick = {
                        annotation.lineRange?.let { onNavigateCode(it.start) }
                    },
                    interactionSource = null,
                    indication = null
                ),
                name = name,
                renderState = uiRenderState.state,
                scale = scale,
                hasFocus = isFocused
            )
        }
    }
}