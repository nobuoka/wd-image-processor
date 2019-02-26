package info.vividcode.wd

import javax.json.JsonObject

data class WebElement(val reference: String) {
    companion object {
        const val IDENTIFIER = "element-6066-11e4-a52e-4f735466cecf"
        const val DEPRECATED_IDENTIFIER = "ELEMENT"

        fun from(obj: JsonObject): WebElement {
            val webElementReference =
                    obj.getJsonString(IDENTIFIER) ?:
                    obj.getJsonString(DEPRECATED_IDENTIFIER) ?:
                    throw RuntimeException("$obj")
            return WebElement(webElementReference.string)
        }
    }
}
