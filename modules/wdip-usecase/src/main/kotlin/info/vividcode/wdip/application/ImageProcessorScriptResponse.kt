package info.vividcode.wdip.application

import info.vividcode.wd.WebElement
import javax.json.JsonNumber
import javax.json.JsonObject
import javax.json.JsonString

data class ImageProcessorScriptResponse(
        val content: Content,
        val statusCode: Int,
        val httpCache: HttpCache?
) {

    companion object {
        fun parseScriptResponse(obj: JsonObject?) =
                ImageProcessorScriptResponse(
                        content = run {
                            (obj?.get("content") as? JsonObject)?.let { c ->
                                when ((c["type"] as? JsonString)?.string) {
                                    "text" -> Content.Text((c["value"] as? JsonString)?.string)
                                    else -> Content.Screenshot(
                                            (c["targetElement"] as? JsonObject)?.let(WebElement.Companion::from),
                                            if ((c["imageType"] as? JsonString)?.string == "jpeg")
                                                Content.Screenshot.ImageType.JPEG
                                            else
                                                Content.Screenshot.ImageType.PNG
                                    )
                                }
                            } ?: Content.Screenshot(
                                    (obj?.get("targetElement") as? JsonObject)?.let(WebElement.Companion::from),
                                    Content.Screenshot.ImageType.PNG)
                        },
                        statusCode = (obj?.get("statusCode") as? JsonNumber)?.intValue() ?: 200,
                        httpCache = (obj?.get("httpCache") as? JsonObject)?.let {
                            HttpCache((it["maxAge"] as? JsonNumber)?.intValue())
                        }
                )
    }

}
