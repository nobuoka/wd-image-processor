package info.vividcode.wdip.ktor

import info.vividcode.wdip.application.WdImageProcessingResultContent
import io.ktor.http.ContentType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

internal class ByteArrayContentTest {

    @Nested
    internal inner class InstanceCreation {
        /**
         * Small PNG bytes.
         * From [The smallest transparent pixel](http://proger.i-forge.net/%D0%9A%D0%BE%D0%BC%D0%BF%D1%8C%D1%8E%D1%82%D0%B5%D1%80/[20121112]%20The%20smallest%20transparent%20pixel.html).
         */
        private val pngBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5ErkJggg=="
        )

        /**
         * Small JPEG bytes.
         * Created from [pngBytes].
         */
        private val jpegBytes = Base64.getDecoder().decode(
                "/9j/4AAQSkZJRgABAQEAYABgAAD/4QA6RXhpZgAATU0AKgAAAAgAA1EQAAEAAAABAQAAAFERAAQAAAABAAAAAFESAAQAAA" +
                        "ABAAAAAAAAAAD/2wBDAAIBAQIBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMB" +
                        "wkODw0MDgsMDAz/2wBDAQICAgMDAwYDAwYMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM" +
                        "DAwMDAwMDAwMDAz/wAARCAABAAEDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRA" +
                        "AAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NT" +
                        "Y3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t" +
                        "7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQF" +
                        "BgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOE" +
                        "l8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoq" +
                        "OkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9/" +
                        "KKKKAP/2Q=="
        )

        @Test
        internal fun fromNull() {
            val content = ByteArrayContent.from(null)

            Assertions.assertEquals(ContentType.parse("application/octet-stream"), content.contentType)
            Assertions.assertArrayEquals(ByteArray(0), content.bytes())
        }

        @Test
        internal fun fromPng() {
            val content = ByteArrayContent.from(WdImageProcessingResultContent.createPng(pngBytes))

            Assertions.assertEquals(ContentType.parse("image/png"), content.contentType)
            Assertions.assertArrayEquals(pngBytes, content.bytes())
        }

        @Test
        internal fun fromJpeg() {
            val content = ByteArrayContent.from(WdImageProcessingResultContent.createJpeg(jpegBytes))

            Assertions.assertEquals(ContentType.parse("image/jpeg"), content.contentType)
            Assertions.assertArrayEquals(jpegBytes, content.bytes())
        }

        @Test
        internal fun fromText() {
            val content = ByteArrayContent.from(WdImageProcessingResultContent.createText("Test string"))

            Assertions.assertEquals(ContentType.parse("text/plain; charset=utf-8"), content.contentType)
            Assertions.assertArrayEquals("Test string".toByteArray(Charsets.UTF_8), content.bytes())
        }
    }

}
