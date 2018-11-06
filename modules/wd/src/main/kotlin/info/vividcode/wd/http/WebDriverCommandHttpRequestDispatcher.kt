package info.vividcode.wd.http

import com.beust.klaxon.JsonObject

interface WebDriverCommandHttpRequestDispatcher {
    fun <T> dispatch(commandHttpRequest: WebDriverCommandHttpRequest, responseHandler: (JsonObject) -> T): T

    interface Factory {
        fun create(baseUrl: String): WebDriverCommandHttpRequestDispatcher
    }

}
