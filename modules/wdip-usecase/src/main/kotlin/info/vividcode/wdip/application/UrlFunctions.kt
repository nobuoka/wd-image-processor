package info.vividcode.wdip.application

import java.nio.charset.StandardCharsets
import java.util.*

fun createHtmlDataUrl(html: String) =
    "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"
