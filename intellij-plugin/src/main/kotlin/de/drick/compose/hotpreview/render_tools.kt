package de.drick.compose.hotpreview

import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.ComposableMethod
import androidx.compose.ui.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import java.lang.reflect.Method
import kotlin.math.min

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
)

fun cropUsingSurface(image: Image, width: Int, height: Int): Image {
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    //val paint = Paint()
    //canvas.drawImageRect(image, Rect(0f, 0f, width.toFloat(), height.toFloat()), paint)
    canvas.drawImage(image, 0f, 0f)
    return surface.makeImageSnapshot()
}

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
fun renderMethod(
    method: Method,
    size: DpSize,
    density: Density,
    isDarkTheme: Boolean
): RenderedImage? {
    val log = Logger.getInstance("renderMethod")
    val theme = if (isDarkTheme) SystemTheme.Dark else SystemTheme.Light
    log.debug("Test render: $method")
    val defaultWidth = 1024f * density.density
    val defaultHeight = 1024f * density.density
    val width = size.width.value * density.density
    val height = size.height.value * density.density
    val widthUndefined = width < 1f
    val heightUndefined = height < 1f
    val renderWidth = if (widthUndefined) defaultWidth.toInt() else width.toInt()
    val renderHeight = if (heightUndefined) defaultHeight.toInt() else height.toInt()
    log.debug("Render size: $renderWidth x $renderHeight")
    repeat(3) {
        try {
            var calculatedSize = IntSize.Zero
            var image = ImageComposeScene(
                width = renderWidth,
                height = renderHeight,
                density = density,
                content = {
                    CompositionLocalProvider(LocalSystemTheme provides theme) {
                        method.invoke(null, currentComposer, 0)
                    }
                }
            ).use { scene ->
                val image = scene.render()
                calculatedSize = scene.calculateContentSize()
                image
            }
            val realWidth = min(calculatedSize.width, renderWidth)
            val realHeight = min(calculatedSize.height, renderHeight)
            // Maybe crop image
            if (widthUndefined || heightUndefined) {
                println("We need to crop the image")
                image = cropUsingSurface(
                    image = image,
                    width = if (widthUndefined) realWidth else renderWidth,
                    height = if (heightUndefined) realHeight else renderHeight,
                )
                println("Cropped image size: ${image.width} x ${image.height}")
            }
            println("Calculated size: $calculatedSize render size: $renderWidth x $renderHeight")
            val placedWidth = realWidth / density.density
            val placedHeight = realHeight / density.density
            println("Rendered size: $placedWidth x $placedHeight")
            return RenderedImage(
                image = image.toComposeImageBitmap(),
                size = DpSize(placedWidth.toInt().dp, placedHeight.toInt().dp)
            )
        } catch (err: Throwable) {
            println("Problem during render!")
            log.warn(err)
        }
    }
    return null
}