package info.vividcode.wd

interface WebDriverCommand {

    class NewSession : WebDriverCommand

    interface SessionCommand {
        val session: WebDriverSession
    }

    data class DeleteSession(override val session: WebDriverSession) :
        SessionCommand
    data class SetWindowRect(override val session: WebDriverSession, val rect: Rect) :
        SessionCommand

    data class SetTimeouts(override val session: WebDriverSession, val timeouts: Timeouts) : SessionCommand

    data class Go(override val session: WebDriverSession, val url: String) :
        SessionCommand
    data class ExecuteAsyncScript(override val session: WebDriverSession, val script: Script) :
        SessionCommand
    data class TakeScreenshot(override val session: WebDriverSession) :
        SessionCommand
    data class TakeElementScreenshot(override val session: WebDriverSession, val targetElement: WebElement) :
        SessionCommand
    data class FindElement(override val session: WebDriverSession, val selector: ElementSelector) :
        SessionCommand

}
