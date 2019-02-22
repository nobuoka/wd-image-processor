package info.vividcode.wd.pool.internal

import info.vividcode.wd.WebDriverError
import info.vividcode.wd.WebDriverSession

internal class WdSessionInfo(
        val correspondingWdRemoteEnd: WdRemoteEnd,
        val session: WebDriverSession
) {

    var numUsed: Int = 0
        private set(value) { field = value }

    suspend fun <T> use(block: suspend (WdSessionInfo) -> T): T {
        var unSupposedErrorOccurred = false
        try {
            return block(this)
        } catch (e: WebDriverError) {
            unSupposedErrorOccurred = when (e) {
                // `script timeout` error is supposed
                is WebDriverError.ScriptTimeout -> false
            }
            throw RuntimeException(e)
        } catch (e: Exception) {
            unSupposedErrorOccurred = true
            throw e
        } finally {
            finishUse(unSupposedErrorOccurred)
        }
    }

    suspend fun finishUse(unSupposedErrorOccurred: Boolean) {
        try {
            numUsed++
            correspondingWdRemoteEnd.returnSession(this, unSupposedErrorOccurred)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
