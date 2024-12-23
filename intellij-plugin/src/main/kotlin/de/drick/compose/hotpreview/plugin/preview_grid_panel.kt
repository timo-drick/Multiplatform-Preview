package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer


@Composable
fun PreviewItem(name: String, annotation: HotPreview, image: RenderedImage?, scale: Float = 1f) {
    val borderStroke = BorderStroke(2.dp, JewelTheme.globalColors.outlines.focused)
    Column(Modifier) {
        val postFix = if (annotation.name.isNotBlank()) " - ${annotation.name}" else ""
        Text("$name $postFix")
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