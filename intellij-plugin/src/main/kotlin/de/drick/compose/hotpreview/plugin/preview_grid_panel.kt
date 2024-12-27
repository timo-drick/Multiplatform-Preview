package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
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
    scale: Float = 1f
) {
    val borderStroke = BorderStroke(2.dp, JewelTheme.globalColors.outlines.focused)
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
    modifier: Modifier = Modifier
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
                    PreviewItem(
                        name = preview.function.name,
                        annotation = annotation,
                        image = preview.image.getOrNull(index),
                        scale = scale
                    )
                }
            }
        }
    }
}