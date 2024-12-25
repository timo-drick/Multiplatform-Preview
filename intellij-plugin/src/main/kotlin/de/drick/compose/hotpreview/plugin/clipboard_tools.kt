package de.drick.compose.hotpreview.plugin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

object ClipboardImage {
    /**
     * Place an image on the system clipboard.
     *
     * @param  image - the image to be added to the system clipboard
     */
    fun write(image: ImageBitmap) {
        val transferable = ImageTransferable(image.toAwtImage())
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    internal class ImageTransferable(private val image: Image) : Transferable {
        @Throws(UnsupportedFlavorException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
            if (isDataFlavorSupported(flavor)) {
                return image
            } else {
                throw UnsupportedFlavorException(flavor)
            }
        }
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor === DataFlavor.imageFlavor
        override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
    }
}