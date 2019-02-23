package info.vividcode.wd.pool.internal

import info.vividcode.wd.Timeouts
import info.vividcode.wd.WebDriverCommand
import info.vividcode.wd.WebDriverCommandExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext

internal class WdRemoteEnd(
        val webDriverCommandExecutor: WebDriverCommandExecutor,
        val webDriverExecutionContext: CoroutineContext,
        val sessionsIdle: MutableSet<WdSessionInfo> = mutableSetOf(),
        val sessionsInUse: MutableSet<WdSessionInfo> = mutableSetOf(),
        val timeouts: Timeouts,
        val maxNumSessions: Int = 1,
        val maxNumSessionUsed: Int = 10
) {

    val onSessionReturnedEventCallbacks = mutableSetOf<CompletableDeferred<Unit>>()

    fun canPublishSession() = sessionsInUse.size < maxNumSessions

    suspend fun publishSession(): WdSessionInfo {
        if (!canPublishSession()) throw RuntimeException(
            "Cannot publish session (Number of sessions in use : ${sessionsInUse.size}," +
                    " Max number of sessions : $maxNumSessions)")

        val session: WdSessionInfo
        if (sessionsIdle.isNotEmpty()) {
            session = sessionsIdle.first()
            sessionsIdle.remove(session)
        } else {
            val wdSession = GlobalScope.async(webDriverExecutionContext) {
                with(webDriverCommandExecutor) {
                    WebDriverCommand.NewSession().execute().also { wdSession ->
                        WebDriverCommand.SetTimeouts(wdSession, timeouts).execute()
                    }
                }
            }.await()
            session = WdSessionInfo(this, wdSession)
        }
        sessionsInUse.add(session)
        return session
    }

    suspend fun returnSession(sessionInfo: WdSessionInfo, forceRelease: Boolean = false) {
        val removed = sessionsInUse.remove(sessionInfo)
        if (!removed) {
            throw RuntimeException("Unknown session")
        }

        if (sessionInfo.numUsed >= maxNumSessionUsed  || forceRelease) {
            GlobalScope.async(webDriverExecutionContext) {
                with(webDriverCommandExecutor) {
                    WebDriverCommand.DeleteSession(sessionInfo.session).execute()
                }
            }.await()
        } else {
            sessionsIdle.add(sessionInfo)
        }

        onSessionReturnedEventCallbacks.forEach { it.complete(Unit) }
        onSessionReturnedEventCallbacks.clear()
    }

}
