package de.drick.compose.hotpreview.plugin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.fileLogger
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import kotlin.time.TimeSource

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

sealed interface RenderState
data class RenderError(val errorMessage: String): RenderState
data object NotRenderedYet: RenderState

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
): RenderState

fun renderPreview(
    renderClassLoader: RenderClassLoaderInstance,
    function: HotPreviewFunction,
    annotation: HotPreviewModel
): RenderState {
    val clazz = renderClassLoader.fileClass
    val functionRef = renderClassLoader.renderFunctionRef
    val renderClassInstance = renderClassLoader.renderClassInstance
    return try {
        LOG.debug("F: $function")
        val ts = TimeSource.Monotonic

        val classLoadingStart = ts.markNow()
        val method = clazz.declaredMethods.find { it.name == function.name }
        requireNotNull(method) { "Unable to find method: ${function.name}" }
        val classLoadingDuration = ts.markNow() - classLoadingStart
        println("Class loading time: $classLoadingDuration")

        val result = functionRef.call(
            renderClassInstance,
            method,
            annotation.widthDp.toFloat(),
            annotation.heightDp.toFloat(),
            annotation.density,
            annotation.fontScale,
            annotation.darkMode,
            annotation.locale,
            true
        )
        when (result) {
            is BufferedImage -> {
                val image = result.toComposeImageBitmap()
                val widthDp = image.width.toFloat() / annotation.density
                val heightDp = image.height.toFloat() / annotation.density
                RenderedImage(
                    image = image,
                    size = DpSize(widthDp.dp, heightDp.dp)
                )
            }
            is ByteArray -> {
                val image = ByteArrayInputStream(result).use {
                    Image.makeFromEncoded(it.readAllBytes())
                }.toComposeImageBitmap()
                val widthDp = image.width.toFloat() / annotation.density
                val heightDp = image.height.toFloat() / annotation.density
                RenderedImage(
                    image = image,
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
