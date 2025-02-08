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

data class RenderedImage(
    val image: ImageBitmap,
    val size: DpSize
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
        val images = function.annotation.map { hpAnnotation ->
            val annotation = hpAnnotation.annotation
            if (functionRef != null) {
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
                val byteArray = result as? ByteArray
                byteArray?.let {
                    val image = ByteArrayInputStream(it).use { ImageIO.read(it) }.toComposeImageBitmap()
                    val widthDp = image.width.toFloat() / annotation.density
                    val heightDp = image.height.toFloat() / annotation.density
                    RenderedImage(
                        image = image,
                        size = DpSize(widthDp.dp, heightDp.dp)
                    )
                }
            } else {
                null
            }
        }
        HotPreviewData(
            function = function,
            image = images
        )
    }
}
