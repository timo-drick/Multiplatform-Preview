package de.drick.compose.hotpreview.plugin.ui.preview_window

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
import de.drick.compose.hotpreview.plugin.ui.preview_window.HotPreviewViewModel.RenderCacheKey
import de.drick.compose.hotpreview.plugin.ui.components.FoldableSection
import de.drick.compose.hotpreview.plugin.ui.components.ScrollbarContainer
import de.drick.compose.hotpreview.plugin.ui.components.forwardMinIntrinsicWidth
import de.drick.compose.hotpreview.plugin.ui.components.intrinsicScrollModifier
import de.drick.compose.hotpreview.plugin.ui.guttericon.DialogGutterIconSettings
import de.drick.compose.hotpreview.plugin.ui.guttericon.GutterIconViewModelI
import de.drick.compose.hotpreview.plugin.ui.guttericon.mockGutterIconViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.modifier.onHover
import kotlin.collections.toSet


@OptIn(ExperimentalResourceApi::class)
@HotPreview(widthDp = 900, heightDp = 1000)
@HotPreview(widthDp = 650, heightDp = 1000, darkMode = false)
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
            onNavigateCode = {},
            onGetGutterIconViewModel = { mockGutterIconViewModel }
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
    requestPreviews: (Set<RenderCacheKey>) -> Map<RenderCacheKey, UIRenderState>,
    onGetGutterIconViewModel: (UIAnnotation) -> GutterIconViewModelI
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
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    ScrollbarContainer(
        modifier = modifier.fillMaxSize(),
        verticalScrollState = verticalScrollState,
        horizontalScrollState = horizontalScrollState
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp)
                .fillMaxWidth()
                .verticalScroll(verticalScrollState)
                .intrinsicScrollModifier(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewList.forEach { preview ->
                if (preview.annotations.size > 1) {
                    FoldableSection(preview.functionName) {
                        PreviewSection(
                            modifier = Modifier.forwardMinIntrinsicWidth(),
                            hasHeader = true,
                            scale = scale,
                            preview = preview,
                            renderStateMap = previewMap,
                            onNavigateCode = onNavigateCode,
                            onGetGutterIconViewModel = onGetGutterIconViewModel
                        )
                    }
                } else {
                    PreviewSection(
                        hasHeader = false,
                        scale = scale,
                        preview = preview,
                        renderStateMap = previewMap,
                        onNavigateCode = onNavigateCode,
                        onGetGutterIconViewModel = onGetGutterIconViewModel
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
    modifier: Modifier = Modifier,
    onNavigateCode: (Int) -> Unit,
    onGetGutterIconViewModel: (UIAnnotation) -> GutterIconViewModelI
) {
    var gutterIconViewModel: GutterIconViewModelI? by remember { mutableStateOf(null) }

    FlowRow(
        modifier = modifier,
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
            val uiRenderState = renderStateMap[annotation.renderCacheKey] ?: UIRenderState(
                widthDp = annotation.renderCacheKey.annotation.widthDp,
                heightDp = annotation.renderCacheKey.annotation.heightDp
            )
            PreviewItem(
                modifier = Modifier.onHover { isFocused = it }.clickable(
                    onClick = {
                        onNavigateCode(annotation.lineRange.start)
                    },
                    interactionSource = null,
                    indication = null
                ),
                name = name,
                renderState = uiRenderState.state,
                scale = scale,
                hasFocus = isFocused,
                onAction = {
                    when (it) {
                        PreviewItemAction.Settings -> {
                            val viewModel = onGetGutterIconViewModel(annotation)
                            gutterIconViewModel = viewModel
                        }
                    }
                }
            )
        }
    }
    DialogGutterIconSettings(
        viewModel = gutterIconViewModel,
        onClose = { gutterIconViewModel = null }
    )
}