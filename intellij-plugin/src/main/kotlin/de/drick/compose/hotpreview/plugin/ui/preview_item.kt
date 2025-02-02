package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.ClipboardImage
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.RenderedImage
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@HotPreview
@HotPreview(darkMode = false)
@Composable
private fun PreviewPreviewItem() {
    val item = remember {
        getHotPreviewDataItem("login_dark")
    }
    SelfPreviewTheme {
        PreviewItem(
            modifier = Modifier.padding(8.dp),
            name = "TestItem",
            annotation = item.function.annotation.first().annotation,
            image = item.image.first()
        )
    }
}

@HotPreview
@HotPreview(darkMode = false)
@Composable
private fun PreviewPreviewItemFocus() {
    val item = remember {
        getHotPreviewDataItem("login_dark")
    }
    SelfPreviewTheme {
        PreviewItem(
            modifier = Modifier.padding(8.dp),
            name = "TestItem",
            annotation = item.function.annotation.first().annotation,
            image = item.image.first(),
            hasFocus = true
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewItem(
    name: String,
    annotation: HotPreviewModel,
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
