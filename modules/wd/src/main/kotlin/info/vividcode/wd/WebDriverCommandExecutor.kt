package info.vividcode.wd

interface WebDriverCommandExecutor :
    NewSessionCommandExecutor, DeleteSessionCommandExecutor,
    SetWindowRectExecutor,
    GoCommandExecutor, ExecuteScriptCommandExecutor,
    TakeScreenshotCommandExecutor, TakeElementScreenshotCommandExecutor,
    FindElementCommandExecutor

interface NewSessionCommandExecutor {
    fun WebDriverCommand.NewSession.execute(): WebDriverSession
}
interface DeleteSessionCommandExecutor {
    fun WebDriverCommand.DeleteSession.execute()

    fun <T> WebDriverSession.use(task: (WebDriverSession) -> T): T {
        val webDriverSession = this
        return AutoCloseable { WebDriverCommand.DeleteSession(webDriverSession).execute() }.use {
            task(webDriverSession)
        }
    }
}

interface SetWindowRectExecutor { fun WebDriverCommand.SetWindowRect.execute() }
interface GoCommandExecutor { fun WebDriverCommand.Go.execute() }
interface ExecuteScriptCommandExecutor { fun WebDriverCommand.ExecuteAsyncScript.execute(): ScriptResult }
interface TakeScreenshotCommandExecutor { fun WebDriverCommand.TakeScreenshot.execute(): ByteArray }
interface TakeElementScreenshotCommandExecutor { fun WebDriverCommand.TakeElementScreenshot.execute(): ByteArray }
interface FindElementCommandExecutor { fun WebDriverCommand.FindElement.execute(): WebElement }
