package info.vividcode.wd.new

interface WebDriverSession : FooCommandExecutor, BarCommandExecutor

interface AutoCloseableWebDriverSession : WebDriverSession, AutoCloseable

interface WebDriverCommandResult<T> {
    fun orThrow(): T
    fun <R> then(next: (T) -> WebDriverCommandResult<R>): WebDriverCommandResult<R>
}

interface WebDriverSessionCommandExecutor {

    fun WebDriverSession.execute(test: String): WebDriverCommandResult<String>

}

interface WebDriverSessionDriver {
    fun <T> withSession(block: WebDriverSessionCommandExecutor.(WebDriverSession) -> T): T
}

interface WebDriverSessionPool {
    fun getSession(): WebDriverSession
}

data class FooCommand(val value: String)
data class FooResult(val value: String)
interface FooCommandExecutor {
    fun execute(command: FooCommand): FooResult
}

data class BarCommand(val value: String)
data class BarResult(val value: String)
interface BarCommandExecutor {
    fun execute(command: BarCommand): BarResult
}


class SimpleFooCommandExecutor : FooCommandExecutor {
    override fun execute(command: FooCommand): FooResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
class SimpleBarCommandExecutor : BarCommandExecutor {
    override fun execute(command: BarCommand): BarResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun main() {
    val session = object :
            FooCommandExecutor by SimpleFooCommandExecutor(),
            BarCommandExecutor by SimpleBarCommandExecutor(),
            AutoCloseableWebDriverSession {
        override fun close() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    /*
    val driver = object : WebDriverSessionDriver {
        override fun WebDriverSession.execute(test: String): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    driver.withSession { session ->
        session.execute("")
                .then { session.execute(it) }
                .then { session.execute(it) }
    }

    session.use { s ->
        s.close()
    }

    val webDriverSessionPool = object : WebDriverSessionPool {
        override fun getSession(): WebDriverSession {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    webDriverSessionPool.getSession().use { session ->
        val foo = session.execute(FooCommand("test"))
        foo.value
    }
    */
}
