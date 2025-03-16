package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@HotPreview(widthDp = 400, heightDp = 400)
@Composable
fun PreviewErrorDialog() {
    SelfPreviewTheme {
        ErrorDialog(
            isVisible = true,
            message = "Preview test message",
            onClose = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErrorDialog(
    isVisible: Boolean,
    message: String,
    onClose: () -> Unit
) {
    val dialogProperties = remember {
        DialogProperties(
            usePlatformDefaultWidth = false
        )
    }
    if (isVisible) {
        Dialog(
            properties = dialogProperties,
            onDismissRequest = onClose
        ) {
            Box(Modifier.background(JewelTheme.globalColors.panelBackground)) {
                VerticallyScrollableContainer {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = message,
                        //color = JewelTheme.globalColors.text.error,
                        style = JewelTheme.editorTextStyle,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Tooltip(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    tooltip = { Text("Close error dialog") }
                ) {
                    IconButton(onClick = onClose) {
                        Icon(AllIconsKeys.General.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}