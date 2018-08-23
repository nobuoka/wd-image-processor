package info.vividcode.wd

/**
 * See : [https://www.w3.org/TR/webdriver/#dfn-error-code](https://www.w3.org/TR/webdriver/#dfn-error-code)
 */
sealed class WebDriverError : RuntimeException() {
    class ScriptTimeout : WebDriverError()
}
