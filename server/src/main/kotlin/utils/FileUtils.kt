/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.tools.imageio.ImageIOUtil
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.max

class FileUtils {

    fun convertPdfToImage(inputStream: InputStream, fileName: String): ByteArray {
        val document = PDDocument.load(inputStream)
        val pdfRenderer = PDFRenderer(document)
        var joinBufferedImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        for (page in 0 until document.numberOfPages) {
            val bImage = pdfRenderer.renderImageWithDPI(page, 300f, ImageType.RGB)
            joinBufferedImage = joinBufferedImage(joinBufferedImage, bImage)
        }
        val byteOutputStream = ByteArrayOutputStream()
        ImageIO.write(joinBufferedImage, "png", byteOutputStream)
        document.close()
        return byteOutputStream.toByteArray()
    }

    // https://stackoverflow.com/a/58031443
    private fun joinBufferedImage(img1: BufferedImage, img2: BufferedImage): BufferedImage {
        //do some calculate first
        val offset = 5
        val wid = max(img1.width, img2.width) + offset
        val height = img1.height + img2.height + offset
        //create a new buffer and draw two image into the new image
        val newImage = BufferedImage(wid, height, BufferedImage.TYPE_INT_RGB)
        val g2 = newImage.createGraphics()
        val oldColor = g2.color
        //fill background
        g2.paint = Color.WHITE
        g2.fillRect(0, 0, wid, height)
        //draw image
        g2.color = oldColor
        g2.drawImage(img1, null, 0, 0)
        g2.drawImage(img2, null, 0, img1.height + offset)
        g2.dispose()
        return newImage
    }
}

fun String.isPdf(): Boolean{
    return this.contentEquals(".pdf")
}

fun String.isImage(): Boolean{
    return this.contentEquals(".jpg") ||
            this.contentEquals("jpeg") ||
            this.contentEquals("png")
}
