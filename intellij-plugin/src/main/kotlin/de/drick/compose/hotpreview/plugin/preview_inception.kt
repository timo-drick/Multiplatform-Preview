package de.drick.compose.hotpreview.plugin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.use
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.Text

@Composable
fun Preview1() {
    Box(Modifier.background(Color.Gray).fillMaxSize()) {
        Text("Hello Preview")
    }
}

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

@Composable
fun renderPreview(size: DpSize, content: @Composable () -> Unit): RenderedImage {
    val d = LocalDensity.current
    val sizePx = with(d) { size.toSize().toIntSize() }
    ImageComposeScene(
        width = sizePx.width,
        height = sizePx.height,
        density = d,
        content = content
    ).use { scene ->
        val image = scene.render()
        val realSize = DpSize((image.width / d.density).dp, (image.height / d.density).dp)
        return RenderedImage(image.toComposeImageBitmap(), realSize)
    }
}