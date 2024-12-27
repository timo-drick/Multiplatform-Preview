@file:Suppress("DEPRECATION")

package de.drick.compose.hotpreview.plugin.selfpreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewData
import de.drick.compose.hotpreview.plugin.HotPreviewFunction
import de.drick.compose.hotpreview.plugin.PreviewGridPanel
import de.drick.compose.hotpreview.plugin.RenderedImage
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi


private fun getMockData() = listOf(
    HotPreviewData(
        function = HotPreviewFunction("CountPoser", listOf(
            HotPreview(name = "dark mode", widthDp = 400, heightDp = 400)
        )),
        image = listOf(getPreviewItem("preview_samples/countposer_dialog.png", 2f))
    )
)

fun getPreviewItem(resourcePath: String, density: Float): RenderedImage {
    val image = useResource(resourcePath, ::loadImageBitmap)
    val size = DpSize((image.width * density).dp, (image.height * density).dp)
    return RenderedImage(image, size)
}

@OptIn(ExperimentalJewelApi::class)
@HotPreview(widthDp = 400, heightDp = 500)
@Composable
fun PreviewPreviewGridPanel() {
    val functionList = remember {
        getMockData()
    }
    /*val preview1 = remember {
        getPreviewItem("preview_samples/countposer_dialog.png", 2f)
    }*/
    SwingBridgeTheme {
        PreviewGridPanel(
            hotPreviewList = functionList,
            scale = .3f
        )
    }
}

