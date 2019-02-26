package info.vividcode.wdip.application

import info.vividcode.wd.WebElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import javax.json.Json
import javax.json.JsonValue

internal class ImageProcessorScriptResponseTest {

    @Nested
    internal inner class ParseScriptResponseTest {
        @Test
        internal fun standard() {
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to emptyMap<String, Any?>(),
                    "statusCode" to 200,
                    "httpCache" to emptyMap<String, Any?>()
            )).build()

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
            val testScriptResponseJsonObject = JsonValue.EMPTY_JSON_OBJECT

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
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to mapOf(
                            "type" to "text"
                    )
            )).build()

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Text(null), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_text_stringValue() {
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to mapOf(
                            "type" to "text",
                            "value" to "Hello, world!"
                    )
            )).build()

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Text("Hello, world!"), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_imageType_jpeg() {
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to mapOf(
                            "type" to "image",
                            "imageType" to "jpeg"
                    )
            )).build()

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Screenshot(null, Content.Screenshot.ImageType.JPEG), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_imageType_png() {
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to mapOf(
                            "type" to "image",
                            "imageType" to "png"
                    )
            )).build()

            val scriptResponse = ImageProcessorScriptResponse.parseScriptResponse(testScriptResponseJsonObject)

            Assertions.assertEquals(
                    ImageProcessorScriptResponse(Content.Screenshot(null, Content.Screenshot.ImageType.PNG), 200, null),
                    scriptResponse
            )
        }

        @Test
        internal fun content_image_targetElement() {
            val testElementReference = "4424a995-daf9-486c-919c-0bae727d3805"

            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "content" to mapOf(
                            "type" to "image",
                            "targetElement" to mapOf(
                                    "element-6066-11e4-a52e-4f735466cecf" to testElementReference
                            )
                    )
            )).build()

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
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "statusCode" to 400
            )).build()

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
            val testScriptResponseJsonObject = Json.createObjectBuilder(mapOf(
                    "httpCache" to mapOf(
                            "maxAge" to 20
                    )
            )).build()

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
