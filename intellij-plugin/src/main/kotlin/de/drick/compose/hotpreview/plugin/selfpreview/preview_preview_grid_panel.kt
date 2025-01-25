@file:Suppress("DEPRECATION")

package de.drick.compose.hotpreview.plugin.selfpreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.HotPreviewData
import de.drick.compose.hotpreview.plugin.HotPreviewFunction
import de.drick.compose.hotpreview.plugin.PreviewGridPanel
import de.drick.compose.hotpreview.plugin.RenderedImage
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi


private fun getMockData() = listOf(
    HotPreviewData(
        function = HotPreviewFunction(
            name = "CountPoser",
            annotation = listOf(
                HotPreviewAnnotation(
                    lineRange = null,
                    HotPreview(name = "dark mode", widthDp = 400, heightDp = 400)
                )
            ),
            lineRange = null
        ),
        image = listOf(getPreviewItem("preview_samples/countposer_dialog.png", 2f))
    )
)

private fun getPreviewItem(resourcePath: String, density: Float): RenderedImage {
    val image = useResource(resourcePath, ::loadImageBitmap)
    val size = DpSize((image.width * density).dp, (image.height * density).dp)
    return RenderedImage(image, size)
}

/**
 * Unfortunately self preview is not possible currently.
 * I think this is because the class loader of the plugin already loaded
 * the classes for showing previews so it will not be reloaded when changed.
 * Maybe it could be possible when changing the parent classloader.
 */
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
            scale = .3f,
            onNavigateCode = {}
        )
    }
}

