package info.vividcode.wdip.application

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal class ImageFunctionsTest {

    @Nested
    internal inner class ConvertImageToJpegTest {
        @Test
        internal fun normal() {
            val testPngImageResourcePath = "/info/vividcode/wdip/application/test-image.png"
            val expectedJpegImageResourcePath = "/info/vividcode/wdip/application/test-image.jpg"

            val pngBytes = javaClass.getResourceAsStream(testPngImageResourcePath).use { it.readBytes() }

            // Act
            val actualJpegBytes = convertImageToJpeg(pngBytes)

            val expectedImage = javaClass.getResourceAsStream(expectedJpegImageResourcePath).use { ImageIO.read(it) }
            val actualImage = ImageIO.read(ByteArrayInputStream(actualJpegBytes))

            Assertions.assertEquals(expectedImage.height, actualImage.height)
            Assertions.assertEquals(expectedImage.width, actualImage.width)

            val diffImage = createDiffImage(expectedImage, actualImage)
            // You can write this image to file as following:
            // ImageIO.write(diffImage, "png", File("diff.png"))

            val brightnessArray = createPixelBrightnessArray(diffImage)
            val brightnessAverage = brightnessArray.sum() / brightnessArray.size
            Assertions.assertTrue(brightnessAverage < 0.03) {
                "Brightness average ($brightnessAverage) should be smaller than 0.03"
            }
        }

        private fun createPixelBrightnessArray(image: BufferedImage): DoubleArray {
            val colorModel = image.colorModel
            val componentNameToIndexMap =
                    (0..(colorModel.colorSpace.numComponents - 1)).toList().map { colorModel.colorSpace.getName(it) to it }.toMap()

            val rgbs = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            return rgbs.map {
                val r = colorModel.getRed(it).toDouble() / ((1 shl colorModel.getComponentSize(componentNameToIndexMap.getValue("Red"))) - 1)
                val g = colorModel.getGreen(it).toDouble() / ((1 shl colorModel.getComponentSize(componentNameToIndexMap.getValue("Green"))) - 1)
                val b = colorModel.getBlue(it).toDouble() / ((1 shl colorModel.getComponentSize(componentNameToIndexMap.getValue("Blue"))) - 1)
                Math.max(Math.max(r, g), b)
            }.toDoubleArray()
        }

        private fun createDiffImage(image1: BufferedImage, image2: BufferedImage): BufferedImage {
            val width = image1.width
            val height = image1.height
            val image1Pixels = image1.getRGB(0, 0, width, height, null, 0, width)
            val image2Pixels = image2.getRGB(0, 0, width, height, null, 0, width)

            val zipped = (image1Pixels zip image2Pixels)
            val defaultColorModel = ColorModel.getRGBdefault()

            val diffImagePixels = zipped.map { pair ->
                val redDiff = calculateColorDiff(pair, defaultColorModel::getRed)
                val greenDiff = calculateColorDiff(pair, defaultColorModel::getGreen)
                val blueDiff = calculateColorDiff(pair, defaultColorModel::getBlue)
                (redDiff shl 16) + (greenDiff shl 8) + blueDiff
            }.toIntArray()

            val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            diffImage.setRGB(0, 0, width, height, diffImagePixels, 0, width)
            return diffImage
        }

        private fun calculateColorDiff(pixelPair: Pair<Int, Int>, colorGetter: (Int) -> Int): Int =
                Math.abs(colorGetter(pixelPair.first) - colorGetter(pixelPair.second))
    }

}
