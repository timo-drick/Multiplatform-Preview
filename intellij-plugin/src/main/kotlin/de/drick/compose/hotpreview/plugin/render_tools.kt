@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package de.drick.compose.hotpreview.plugin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.fileLogger
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.reflect.full.declaredFunctions

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

sealed interface RenderState
data class RenderError(val errorMessage: String): RenderState
data object NotRenderedYet: RenderState

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
): RenderState

data class HotPreviewData(
    val function: HotPreviewFunction,
    val image: List<RenderState>,
)

private const val previewRenderImplFqn = "de.drick.compose.hotpreview.RenderPreviewImpl"

fun renderPreview(
    classLoader: ClassLoader,
    fileClassName: String,
    previewList: List<HotPreviewFunction>
): List<HotPreviewData> {
    return previewList.map { function ->
        LOG.debug("F: $function")
        val renderClazz = classLoader.loadClass(previewRenderImplFqn).kotlin
        val renderClassInstance = renderClazz.constructors.first().call()
        val functionRef = renderClazz.declaredFunctions.find {
            it.name == "render"
        }
        LOG.debug(renderClassInstance.toString())
        LOG.debug(functionRef.toString())
        requireNotNull(functionRef) { "Unable to find method: $previewRenderImplFqn:render" }
        val images: List<RenderState> = function.annotation.map { hpAnnotation ->
            val annotation = hpAnnotation.annotation
            try {
                val result = functionRef.call(
                    renderClassInstance,
                    fileClassName,
                    function.name,
                    annotation.widthDp.toFloat(),
                    annotation.heightDp.toFloat(),
                    annotation.density,
                    annotation.fontScale,
                    annotation.darkMode,
                    true
                )
                when (result) {
                    is ByteArray -> {
                        val image = ByteArrayInputStream(result).use { ImageIO.read(it) }.toComposeImageBitmap()
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
        HotPreviewData(
            function = function,
            image = images
        )
    }
}
