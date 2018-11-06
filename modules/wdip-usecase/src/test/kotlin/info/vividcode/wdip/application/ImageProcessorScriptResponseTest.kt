package info.vividcode.wdip.application

import com.beust.klaxon.JsonObject
import info.vividcode.wd.WebElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ImageProcessorScriptResponseTest {

    @Nested
    internal inner class ParseScriptResponseTest {
        @Test
        internal fun standard() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(),
                    "statusCode" to 200,
                    "httpCache" to JsonObject()
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(null, Content.Screenshot.ImageType.PNG),
                            200,
                            HttpCache(maxAge = null)
                    ),
                    scriptResponse
            )
        }

        @Test
        internal fun nullParameter() {
            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(null)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 200, null
                    ),
                    scriptResponse
            )
        }

        @Test
        internal fun emptyJson() {
            val testScriptResponseJsonObject = JsonObject()

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 200, null
                    ),
                    scriptResponse
            )
        }

        @Test
        internal fun content_text_null() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(mapOf(
                            "type" to "text"
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Text(null), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_text_stringValue() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(mapOf(
                            "type" to "text",
                            "value" to "Hello, world!"
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Text("Hello, world!"), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_imageType_jpeg() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(mapOf(
                            "type" to "image",
                            "imageType" to "jpeg"
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Screenshot(null, Content.Screenshot.ImageType.JPEG), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_imageType_png() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(mapOf(
                            "type" to "image",
                            "imageType" to "png"
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_targetElement() {
            val testElementReference = "4424a995-daf9-486c-919c-0bae727d3805"

            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "content" to JsonObject(mapOf(
                            "type" to "image",
                            "targetElement" to JsonObject(mapOf(
                                    "element-6066-11e4-a52e-4f735466cecf" to testElementReference
                            ))
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(WebElement(testElementReference), Content.Screenshot.ImageType.PNG),
                            200,
                            null
                    ),
                    scriptResponse
            )
        }

        @Test
        internal fun statusCode() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "statusCode" to 400
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 400, null
                    ),
                    scriptResponse
            )
        }

        @Test
        internal fun httpCache() {
            val testScriptResponseJsonObject = JsonObject(mapOf(
                    "httpCache" to JsonObject(mapOf(
                            "maxAge" to 20
                    ))
            ))

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(
                            Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 200, HttpCache(20)
                    ),
                    scriptResponse
            )
        }
    }

}
