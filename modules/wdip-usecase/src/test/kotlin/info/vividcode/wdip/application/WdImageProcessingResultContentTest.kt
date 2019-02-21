package info.vividcode.wdip.application

import org.apache.commons.codec.binary.Hex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class WdImageProcessingResultContentTest {

    companion object {
        /**
         * This data is from [The smallest transparent pixel](http://proger.i-forge.net/%D0%9A%D0%BE%D0%BC%D0%BF%D1%8C%D1%8E%D1%82%D0%B5%D1%80/[20121112]%20The%20smallest%20transparent%20pixel.html).
         */
        private val testSmallPngBytes = """
            89 50 4E 47 0D 0A 1A 0A 00 00 00 0D 49 48 44 52 00 00 00 01
            00 00 00 01 08 00 00 00 00 3A 7E 9B 55 00 00 00 0A 49 44 41
            54 18 57 63 F8 0F 00 01 01 01 00 5A 4D 6F F1 00 00 00 00 49
            45 4E 44 AE 42 60 82
        """.replace(Regex("\\s"), "").let(Hex::decodeHex)

        /**
         * This data is from [What is the smallest valid jpeg file size (in bytes)](https://stackoverflow.com/questions/2253404/what-is-the-smallest-valid-jpeg-file-size-in-bytes).
         */
        private val testSmallJpegBytes = """
            FF D8 FF E0 00 10 4A 46 49 46 00 01 01 01 00 48 00 48 00 00
            FF DB 00 43 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
            FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
            FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
            FF FF FF FF FF FF FF FF FF FF C2 00 0B 08 00 01 00 01 01 01
            11 00 FF C4 00 14 10 01 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 FF DA 00 08 01 01 00 01 3F 10
        """.replace(Regex("\\s"), "").let(Hex::decodeHex)
    }

    @Nested
    internal inner class CreateTest {
        @Test
        internal fun createText() {
            val content = WdImageProcessingResultContent.createText("Hi!")

            Assertions.assertEquals(WdImageProcessingResultContent.Type.TEXT, content.type)
            Assertions.assertArrayEquals(byteArrayOf(72, 105, 33), content.bytes)
        }

        @Test
        internal fun createPng() {
            val content = WdImageProcessingResultContent.createPng(testSmallPngBytes)

            Assertions.assertEquals(WdImageProcessingResultContent.Type.IMAGE_PNG, content.type)
            Assertions.assertArrayEquals(testSmallPngBytes, content.bytes)
        }

        @Test
        internal fun createJpeg() {
            val content = WdImageProcessingResultContent.createJpeg(testSmallJpegBytes)

            Assertions.assertEquals(WdImageProcessingResultContent.Type.IMAGE_JPEG, content.type)
            Assertions.assertArrayEquals(testSmallJpegBytes, content.bytes)
        }
    }

    @Test
    internal fun equals_sameValue() {
        val content = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertTrue(content == content)
        Assertions.assertTrue(content == WdImageProcessingResultContent.createText("Hello, world!"))
    }

    @Test
    internal fun equals_differentValue() {
        val content = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertFalse(content == WdImageProcessingResultContent.createText("Hello, world?"))
    }

    @Test
    internal fun hashCode_sameValue() {
        val content1 = WdImageProcessingResultContent.createText("Hello, world!")
        val content2 = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertTrue(content1.hashCode() == content2.hashCode())
    }

    @Test
    internal fun hashCode_differentValue() {
        val content1 = WdImageProcessingResultContent.createText("Hello, world!")
        val content2 = WdImageProcessingResultContent.createText("Hello, world?")

        Assertions.assertFalse(content1.hashCode() == content2.hashCode())
    }

}
