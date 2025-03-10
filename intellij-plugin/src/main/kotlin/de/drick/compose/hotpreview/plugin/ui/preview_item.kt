package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.*
import de.drick.compose.hotpreview.plugin.ui.jewel.Typography
import de.drick.compose.utils.ClipboardImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalResourceApi::class)
@HotPreview
@HotPreview(darkMode = false)
@Composable
private fun PreviewPreviewItem() {
    val env = rememberResourceEnvironment()
    SelfPreviewTheme {
        PreviewItem(
            modifier = Modifier.padding(8.dp),
            name = "TestItem",
            renderState = getPreviewItem(env, SamplePreviewItem.login_dark)
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@HotPreview
@HotPreview(darkMode = false)
@Composable
private fun PreviewPreviewItemFocus() {
    val env = rememberResourceEnvironment()
    SelfPreviewTheme {
        PreviewItem(
            modifier = Modifier.padding(8.dp),
            name = "TestItem",
            renderState = getPreviewItem(env, SamplePreviewItem.login_dark),
            hasFocus = true
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@HotPreview(name = "dark", widthDp = 400, heightDp = 200)
@HotPreview(name = "light", widthDp = 400, heightDp = 200, darkMode = false)
@Composable
fun PreviewPreviewItemError() {
    val env = rememberResourceEnvironment()
    SelfPreviewTheme {
        PreviewItem(
            modifier = Modifier.padding(8.dp),
            name = "TestItem",
            renderState = getPreviewItem(env, SamplePreviewItem.error_test)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewItem(
    name: String,
    renderState: RenderState,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    hasFocus: Boolean = false,
) {
    val focusStroke = BorderStroke(2.dp, JewelTheme.globalColors.outlines.focused)
    val borderModifier = if (hasFocus) Modifier.border(focusStroke) else Modifier
    val borderStroke = BorderStroke(1.dp, Color.Black)
    Column(modifier.width(IntrinsicSize.Min)) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                style = Typography.labelTextStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when (renderState) {
                NotRenderedYet -> {}
                is RenderError -> {
                    var showDialog by remember { mutableStateOf(false) }
                    Tooltip(tooltip = { Text("Copy image to clipboard") }) {
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(AllIconsKeys.General.ErrorDialog, contentDescription = "Copy")
                        }
                    }
                    if (showDialog) {
                        ErrorDialog(
                            isVisible = showDialog,
                            message = renderState.errorMessage,
                            onClose = { showDialog = false }
                        )
                    }
                }
                is RenderedImage -> {
                    Tooltip(tooltip = { Text("Copy image to clipboard") }) {
                        IconButton(onClick = {
                            ClipboardImage.write(renderState.image)
                        }) {
                            Icon(AllIconsKeys.General.Copy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (renderState) {
            NotRenderedYet -> {
                CircularProgressIndicator()
            }
            is RenderedImage -> {
                Image(
                    modifier = Modifier
                        .requiredSize(renderState.size * scale)
                        .then(borderModifier)
                        .border(borderStroke),
                    bitmap = renderState.image,
                    contentScale = ContentScale.Crop,
                    contentDescription = "Preview of $name"
                )
            }
            is RenderError -> {
                Column(Modifier.padding(8.dp).widthIn(min = 200.dp).heightIn(max = 400.dp)) {
                    Text(
                        text = "Unable to render preview!",
                        color = JewelTheme.globalColors.text.error,
                        style = JewelTheme.editorTextStyle
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = renderState.errorMessage,
                        color = JewelTheme.globalColors.text.error,
                        style = JewelTheme.editorTextStyle,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
