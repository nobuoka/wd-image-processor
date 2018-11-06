package info.vividcode.wdip.application

import info.vividcode.wd.WebElement

sealed class Content {
    data class Screenshot(val targetElement: WebElement?, val imageType: ImageType) : Content() {
        enum class ImageType { PNG, JPEG }
    }
    data class Text(val value: String?) : Content()
}
