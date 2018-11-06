package info.vividcode.wdip.application

import com.beust.klaxon.JsonObject
import info.vividcode.wd.WebElement

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
                                when (c.get("type") as? String) {
                                    "text" -> Content.Text(c["value"] as? String)
                                    else -> Content.Screenshot(
                                            (c["targetElement"] as? JsonObject)?.let(WebElement.Companion::from),
                                            if (c["imageType"] == "jpeg") Content.Screenshot.ImageType.JPEG else Content.Screenshot.ImageType.PNG
                                    )
                                }
                            } ?: Content.Screenshot(
                                    (obj?.get("targetElement") as? JsonObject)?.let(WebElement.Companion::from),
                                    Content.Screenshot.ImageType.PNG)
                        },
                        statusCode = (obj?.get("statusCode") as? Int) ?: 200,
                        httpCache = (obj?.get("httpCache") as? JsonObject)?.let {
                            HttpCache(it["maxAge"] as? Int)
                        }
                )
    }

}
