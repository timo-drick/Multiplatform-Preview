package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewItem(
    name: String,
    annotation: HotPreview,
    image: RenderedImage?,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    hasFocus: Boolean = false,
) {
    val focusStroke = BorderStroke(2.dp, JewelTheme.globalColors.outlines.focused)
    val borderModifier = if (hasFocus) Modifier.border(focusStroke) else Modifier
    val borderStroke = BorderStroke(1.dp, Color.Black)
    Column(modifier.width(IntrinsicSize.Min)) {
        val postFix = if (annotation.name.isNotBlank()) " - ${annotation.name}" else ""
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = "$name $postFix",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (image != null) {
                Tooltip(tooltip = { Text("Copy image to clipboard") }) {
                    IconButton(onClick = {
                        ClipboardImage.write(image.image)
                    }) {
                        Icon(AllIconsKeys.General.Copy, contentDescription = "Copy")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (image != null) {
            Image(
                modifier = Modifier
                    .requiredSize(image.size * scale)
                    .then(borderModifier)
                    .border(borderStroke),
                bitmap = image.image,
                contentScale = ContentScale.Crop,
                contentDescription = "Preview of $name"
            )
        }
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
                    var isSelected by remember { mutableStateOf(false) }
                    PreviewItem(
                        modifier = Modifier.onHover { isSelected = it }.clickable(
                            onClick = {
                                annotation.lineRange?.let { onNavigateCode(it.start) }
                            },
                            interactionSource = null,
                            indication = null
                        ),
                        name = preview.function.name,
                        annotation = annotation.annotation,
                        image = preview.image.getOrNull(index),
                        scale = scale,
                        hasFocus = isSelected
                    )
                }
            }
        }
    }
}