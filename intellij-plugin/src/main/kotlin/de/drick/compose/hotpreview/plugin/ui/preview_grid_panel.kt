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
import de.drick.compose.hotpreview.plugin.HotPreviewData
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.*


@HotPreview(widthDp = 650, heightDp = 1400)
@HotPreview(widthDp = 650, heightDp = 1400, darkMode = false)
@Composable
private fun PreviewPreviewGridPanel() {
    val data = remember {
        getMockData()
    }
    SelfPreviewTheme {
        PreviewGridPanel(
            hotPreviewList = data,
            scale = 1f,
            onNavigateCode = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewGridPanel(
    hotPreviewList: List<HotPreviewData>,
    scale: Float,
    modifier: Modifier = Modifier,
    onNavigateCode: (Int) -> Unit
) {
    VerticallyScrollableContainer(
        modifier = modifier.fillMaxSize()
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hotPreviewList.forEach { preview ->
                if (preview.function.annotation.size > 1) {
                    FoldableSection(preview.function.name) {
                        PreviewSection(
                            hasHeader = true,
                            scale = scale,
                            preview = preview,
                            onNavigateCode = onNavigateCode
                        )
                    }
                } else {
                    PreviewSection(
                        hasHeader = false,
                        scale = scale,
                        preview = preview,
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
    preview: HotPreviewData,
    onNavigateCode: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        preview.function.annotation.forEachIndexed { index, annotation ->
            var isFocused by remember { mutableStateOf(false) }

            val fName = preview.function.name
            val aName = annotation.annotation.name
            val name = when {
                hasHeader && aName.isNotBlank() -> aName
                aName.isNotBlank() -> "$fName - $aName"
                else -> fName
            }
            PreviewItem(
                modifier = Modifier.onHover { isFocused = it }.clickable(
                    onClick = {
                        annotation.lineRange?.let { onNavigateCode(it.start) }
                    },
                    interactionSource = null,
                    indication = null
                ),
                name = name,
                renderState = preview.image[index],
                scale = scale,
                hasFocus = isFocused
            )
        }
    }
}