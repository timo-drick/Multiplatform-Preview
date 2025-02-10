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
                preview.function.annotation.forEachIndexed { index, annotation ->
                    var isFocused by remember { mutableStateOf(false) }
                    PreviewItem(
                        modifier = Modifier.onHover { isFocused = it }.clickable(
                            onClick = {
                                annotation.lineRange?.let { onNavigateCode(it.start) }
                            },
                            interactionSource = null,
                            indication = null
                        ),
                        name = preview.function.name,
                        annotation = annotation.annotation,
                        renderState = preview.image[index],
                        scale = scale,
                        hasFocus = isFocused
                    )
                }
            }
        }
    }
}