package info.vividcode.wdip.application

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.imageio.ImageIO

/**
 * * See : https://stackoverflow.com/questions/9340569/jpeg-image-with-wrong-colors
 * * See : https://www.mkyong.com/java/convert-png-to-jpeg-image-file-in-java/
 */
fun convertImageToJpeg(image: ByteArray): ByteArray {
    val bufferedImage = ImageIO.read(ByteArrayInputStream(image)) ?: throw RuntimeException("Converting PNG image to JPEG is failed")

    val rendered = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_INT_RGB)
    rendered.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null)

    val output = ByteArrayOutputStream()
    ImageIO.write(rendered, "jpeg", output)
    return output.toByteArray()
}

fun <T : OutputStream> cropImage(imageInputStream: InputStream, screenshotRect: ScreenshotRect, imageOutputStream: T): T =
    imageOutputStream.also {
        val originalImage = ImageIO.read(imageInputStream)
        val croppedImage = originalImage.getSubimage(screenshotRect.x, screenshotRect.y, screenshotRect.width, screenshotRect.height)
        val ok = ImageIO.write(croppedImage, "png", it)
        if (!ok) throw RuntimeException("No appropriate writer is found for ImageIO")
    }
