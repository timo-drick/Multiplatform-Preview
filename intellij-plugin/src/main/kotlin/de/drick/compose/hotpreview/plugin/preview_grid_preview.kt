package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.Text

@OptIn(ExperimentalJewelApi::class)
@HotPreview(name = "PreviewItem", widthDp = 500, heightDp = 300)
@Composable
fun PreviewPreviewItem() {
    SwingBridgeTheme {
        val renderedImage = renderPreview(DpSize(300.dp, 400.dp)) {
            SwingBridgeTheme {
                Preview2()
            }
        }
        val data = HotPreview(
            name = "Test image",
        )
        PreviewItem("Test", data, renderedImage)
    }
}

@Composable
fun Preview2() {
    Box(Modifier.background(Color.Gray).fillMaxSize()) {
        Text("Hello Preview")
    }
}
