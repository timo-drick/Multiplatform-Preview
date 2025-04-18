package de.drick.compose.hotpreview.plugin.service

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
data class NotRenderedYet(
    val widthDp: Int,
    val heightDp: Int
): RenderState

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
): RenderState

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
