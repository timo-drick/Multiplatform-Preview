package de.drick.compose.hotpreview.plugin.service

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.fileLogger
import de.drick.compose.window_insets_simulation.WindowInsetsDeviceSimulation
import de.drick.compose.window_insets_simulation.toWindowInsetsDeviceConfig
import de.drick.compose.window_insets_simulation.toWindowInsetsString
import java.awt.image.BufferedImage
import kotlin.time.TimeSource

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

sealed interface RenderState
data class RenderError(val errorMessage: String): RenderState
data class NotRenderedYet(
    val widthDp: Int,
    val heightDp: Int
): RenderState

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
): RenderState

@OptIn(InternalComposeUiApi::class)
fun renderPreview(
    renderClassLoader: RenderClassLoaderInstance,
    renderKey: RenderCacheKey
): RenderState {
    val functionName = renderKey.name
    val annotation = renderKey.annotation
    val clazz = renderClassLoader.fileClass
    val functionRef = renderClassLoader.renderFunctionRef
    val renderClassInstance = renderClassLoader.renderClassInstance
    val parameter: Any? = renderKey.parameter
    return try {
        println("F: $functionName")
        val ts = TimeSource.Monotonic

        val classLoadingStart = ts.markNow()
        val method = clazz.declaredMethods.find { it.name == functionName }
        requireNotNull(method) { "Unable to find method: $functionName" }
        val classLoadingDuration = ts.markNow() - classLoadingStart
        println("Class loading time: $classLoadingDuration")
        val insetSimulation = annotation.toWindowInsetsDeviceConfig()
        val windowInsets = insetSimulation.toWindowInsetsString()
        val backgroundColor: Color? = if (annotation.backgroundColor > 0) Color(annotation.backgroundColor) else null

        val result = functionRef.call(
            renderClassInstance,
            method,
            parameter,
            annotation.widthDp.toFloat(),
            annotation.heightDp.toFloat(),
            annotation.density,
            annotation.fontScale,
            annotation.darkMode,
            annotation.locale,
            annotation.layoutDirectionRTL,
            annotation.inspectionMode,
            windowInsets
        )
        when (result) {
            is BufferedImage -> {
                val image = result.toComposeImageBitmap()
                val widthDp = image.width.toFloat() / annotation.density
                val heightDp = image.height.toFloat() / annotation.density
                val theme = if (annotation.darkMode) SystemTheme.Dark else SystemTheme.Light
                val layoutDirection = if (annotation.layoutDirectionRTL) LayoutDirection.Rtl else LayoutDirection.Ltr
                val systemUiImage = ImageComposeScene(
                    width = image.width,
                    height = image.height,
                    density = Density(annotation.density, annotation.fontScale),
                    content = {
                        CompositionLocalProvider(
                            LocalSystemTheme provides theme,
                            LocalLayoutDirection provides layoutDirection
                        ) {
                            WindowInsetsDeviceSimulation(insetSimulation, annotation)
                        }
                    }
                ).render().toComposeImageBitmap()

                val combinedBitmap = combineImageBitmaps(
                    first = image,
                    second = systemUiImage,
                    backgroundColor = backgroundColor,
                    width = image.width,
                    height = image.height
                )
                RenderedImage(
                    image = combinedBitmap,
                    size = DpSize(widthDp.dp, heightDp.dp)
                )
            }
            is String -> {
                println("render method returned string")
                println(result)
                RenderError(result)
            }
            else -> {
                RenderError("Unexpected error during rendering!")
            }
        }
    } catch (err: Throwable) {
        RenderError(err.message ?: err.stackTraceToString())
    }
}

fun combineImageBitmaps(
    first: ImageBitmap,
    second: ImageBitmap,
    backgroundColor: Color?,
    width: Int,
    height: Int
): ImageBitmap {
    val combinedBitmap = ImageBitmap(width, height)
    Canvas(combinedBitmap).let { canvas ->
        if (backgroundColor != null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = backgroundColor })
        }
        canvas.drawImage(first, Offset.Zero, Paint())
        canvas.drawImage(second, Offset.Zero, Paint())
    }
    return combinedBitmap
}