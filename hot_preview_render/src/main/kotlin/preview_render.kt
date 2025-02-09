package de.drick.compose.hotpreview

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import kotlin.math.min

fun cropUsingSurface(image: Image, width: Int, height: Int): Image {
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    canvas.drawImage(image, 0f, 0f)
    return surface.makeImageSnapshot()
}

class RenderPreviewImpl {
    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    fun render(
        clazzFqn: String,
        methodName: String,
        widthDp: Float,
        heightDp: Float,
        density: Float,
        fontScale: Float,
        isDarkTheme: Boolean,
        isInspectionMode: Boolean
    ): ByteArray? {
        val theme = if (isDarkTheme) SystemTheme.Dark else SystemTheme.Light
        val defaultWidth = 1024f * density
        val defaultHeight = 1024f * density
        val widthPx = widthDp * density
        val heightPx = heightDp * density
        val widthUndefined = widthPx < 1f
        val heightUndefined = heightPx < 1f
        val renderWidth = if (widthUndefined) defaultWidth.toInt() else widthPx.toInt()
        val renderHeight = if (heightUndefined) defaultHeight.toInt() else heightPx.toInt()
        println("Render size: $renderWidth x $renderHeight")
        val classLoader = Composer::class.java.classLoader
        val clazz = classLoader.loadClass(clazzFqn)
        val method = clazz.declaredMethods.find { it.name == methodName }
        method?.isAccessible = true
        if (method == null) return null
        try {
            var calculatedSize = IntSize.Zero
            var image = ImageComposeScene(
                width = renderWidth,
                height = renderHeight,
                density = Density(density, fontScale),
                content = {
                    CompositionLocalProvider(
                        LocalSystemTheme provides theme,
                        LocalInspectionMode provides isInspectionMode
                    ) {
                        method.invoke(null, currentComposer, 0)
                    }
                }
            ).use { scene ->
                val image = scene.render(1000 * 1000)
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
            val placedWidth = realWidth / density
            val placedHeight = realHeight / density
            println("Rendered size: $placedWidth x $placedHeight")
            return image.encodeToData(EncodedImageFormat.WEBP)?.bytes
        } catch (err: Throwable) {
            println("Problem during render!")
            err.printStackTrace()
        }
        return null
    }
}
