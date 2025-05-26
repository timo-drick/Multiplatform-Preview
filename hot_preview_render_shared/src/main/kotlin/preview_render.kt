package de.drick.compose.hotpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import java.lang.reflect.Method
import kotlin.math.min
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

fun cropUsingSurface(image: Image, width: Int, height: Int): Image {
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    canvas.drawImage(image, 0f, 0f)
    return surface.makeImageSnapshot()
}

class RenderPreviewImpl {
    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    fun render(
        method: Method,
        parameter: Any?,
        widthDp: Float,
        heightDp: Float,
        density: Float,
        fontScale: Float,
        isDarkTheme: Boolean,
        locale: String,
        layoutDirectionRTL: Boolean,
        isInspectionMode: Boolean,
        windowInsets: String
    ): Any {
        val theme = if (isDarkTheme) SystemTheme.Dark else SystemTheme.Light
        val layoutDirection = if (layoutDirectionRTL) LayoutDirection.Rtl else LayoutDirection.Ltr
        val defaultWidth = 1024f * density
        val defaultHeight = 1024f * density
        val widthPx = widthDp * density
        val heightPx = heightDp * density
        val widthUndefined = widthPx < 1f
        val heightUndefined = heightPx < 1f
        val renderWidth = if (widthUndefined) defaultWidth.toInt() else widthPx.toInt()
        val renderHeight = if (heightUndefined) defaultHeight.toInt() else heightPx.toInt()
        //println("Render size: $renderWidth x $renderHeight")
        WindowInsetsProvider.setInsetsFromString(windowInsets, density)
        val ts = TimeSource.Monotonic
        val beginComposeScene = ts.markNow()
        try {
            method.isAccessible = true
            var calculatedSize = IntSize.Zero
            var image: Image = ImageComposeScene(
                width = renderWidth,
                height = renderHeight,
                density = Density(density, fontScale),
                content = {
                    CompositionLocalProvider(
                        LocalSystemTheme provides theme,
                        LocalInspectionMode provides isInspectionMode,
                        LocalLayoutDirection provides layoutDirection,
                    ) {
                        OverrideEnv(locale = locale) {
                            if (parameter != null) {
                                method.invoke(null, parameter, currentComposer, 0)
                            } else {
                                method.invoke(null, currentComposer, 0)
                            }
                        }
                    }
                }
            ).use { scene ->
                val image = scene.render()
                calculatedSize = scene.calculateContentSize()
                image
            }
            val beginScale = ts.markNow()
            val renderTime = beginScale - beginComposeScene
            println("Render time: $renderTime")
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
            //println("Calculated size: $calculatedSize render size: $renderWidth x $renderHeight")
            val placedWidth = realWidth / density
            val placedHeight = realHeight / density
            //println("Rendered size: $placedWidth x $placedHeight")
            val scaleTime = ts.markNow() - beginScale
            println("Scale time: $scaleTime")
            val (awtImage, duration) = measureTimedValue {
                //image.peekPixels()?.buffer?.bytes
                image.toComposeImageBitmap().toAwtImage()
                //image.encodeToData(EncodedImageFormat.JPEG)?.bytes ?: "Unable to encode rendered preview!"
            }
            println("Encoding time: $duration")
            return awtImage
        } catch (err: Throwable) {
            println("Problem during render!")
            err.printStackTrace()
            return err.cause?.stackTraceToString() ?: err.stackTraceToString()
        }
    }
}