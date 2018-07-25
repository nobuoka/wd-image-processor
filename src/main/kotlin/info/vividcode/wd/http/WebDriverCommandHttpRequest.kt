package info.vividcode.wd.http

data class WebDriverCommandHttpRequest(
        val method: String,
        val path: String,
        val requestContent: String?
)