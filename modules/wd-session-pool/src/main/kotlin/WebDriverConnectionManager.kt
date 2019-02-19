package info.vividcode.wd.pool

import info.vividcode.wd.*
import info.vividcode.wd.http.WebDriverCommandExecutor
import info.vividcode.wd.http.WebDriverCommandHttpRequestDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

class WebDriverConnectionManager(
        webDriverCommandHttpRequestDispatcherFactory: WebDriverCommandHttpRequestDispatcher.Factory,
        webDriverBaseUrls: Collection<String>,
        webDriverSessionCapacity: Int,
        webDriverTimeouts: Timeouts
) {

    private val wdRemoteEndManagingActorMap: Map<String, WdRemoteEndManagingActor>

    private val webDriverManagerContext: CoroutineContext = newSingleThreadContext("WebDriverManagerContext")
    private val webDriverExecutionContext: CoroutineContext =
        newFixedThreadPoolContext(webDriverBaseUrls.size, "WebDriverConnectionContext")

    private val sessionRequestChannel = Channel<CompletableDeferred<WdSessionInfo>>(100)

    init {
        wdRemoteEndManagingActorMap = webDriverBaseUrls.map { webDriverBaseUrl ->
            webDriverBaseUrl to
                    WdRemoteEndManagingActor(
                            sessionRequestChannel,
                            WdRemoteEnd(
                                    WebDriverCommandExecutor(
                                            webDriverCommandHttpRequestDispatcherFactory.create(webDriverBaseUrl)
                                    ),
                                    webDriverExecutionContext,
                                    timeouts = webDriverTimeouts,
                                    maxNumSessionUsed = webDriverSessionCapacity
                            )
                    )
        }.toMap()
        wdRemoteEndManagingActorMap.values.forEach { it.start(webDriverManagerContext) }
    }

    suspend fun checkAllWebDriverRemoteEndsAvailable(): List<Boolean> =
        wdRemoteEndManagingActorMap.values.map {
            GlobalScope.async(webDriverManagerContext) { it.checkWebDriverRemoteEndAvailable() }
        }.map {
            try {
                it.await()
            } catch (rawException: Exception) {
                val e = RuntimeException(rawException)
                e.printStackTrace()
                false
            }
        }

    suspend fun checkWebDriverRemoteEndAvailable(url: String): Boolean? {
        val v = wdRemoteEndManagingActorMap[url] ?: return null
        val r = GlobalScope.async(webDriverManagerContext) { v.checkWebDriverRemoteEndAvailable() }
        return try {
            r.await()
        } catch (rawException: Exception) {
            val e = RuntimeException(rawException)
            e.printStackTrace()
            false
        }
    }

    suspend fun <T> withSession(block: info.vividcode.wd.WebDriverCommandExecutor.(session: WebDriverSession) -> T): T =
        withContext(webDriverManagerContext) {
            val sessionDeferred = CompletableDeferred<WdSessionInfo>()
            sessionRequestChannel.send(sessionDeferred)
            sessionDeferred.await().use { sessionInfo ->
                GlobalScope.async(webDriverExecutionContext) {
                    block(sessionInfo.correspondingWdRemoteEnd.webDriverCommandExecutor, sessionInfo.session)
                }.await()
            }
        }

    private class WdRemoteEndManagingActor(
            private val sessionRequestChannel: Channel<CompletableDeferred<WdSessionInfo>>,
            private val wdRemoteEnd: WdRemoteEnd
    ) {
        private val jobReference = AtomicReference<Job?>()
        private val healthCheckRequestChannel = Channel<CompletableDeferred<Boolean>>(10)

        /** Must be run on managerContext. */
        suspend fun checkWebDriverRemoteEndAvailable(): Boolean =
            if (jobReference.get()?.isActive != true) {
                false
            } else {
                val resultDeferred = CompletableDeferred<Boolean>()
                healthCheckRequestChannel.send(resultDeferred)
                resultDeferred.await()
            }

        fun start(managerContext: CoroutineContext) {
            val job = GlobalScope.launch(managerContext) {
                while (true) {
                    if (wdRemoteEnd.canPublishSession()) {
                        select {
                            healthCheckRequestChannel.onReceive { request ->
                                try {
                                    val result = wdRemoteEnd.publishSession().use { sessionInfo ->
                                        withContext(sessionInfo.correspondingWdRemoteEnd.webDriverExecutionContext) {
                                            WebDriverHealthChecker.checkAvailability(
                                                    sessionInfo.correspondingWdRemoteEnd.webDriverCommandExecutor,
                                                    sessionInfo.session
                                            )
                                        }
                                    }
                                    request.complete(result)
                                } catch (e: Exception) {
                                    request.completeExceptionally(e)
                                }
                            }
                            sessionRequestChannel.onReceive { request ->
                                try {
                                    val session = wdRemoteEnd.publishSession()
                                    request.complete(session)
                                } catch (e: Exception) {
                                    request.completeExceptionally(e)
                                }
                            }
                        }
                    } else {
                        val onReturned = CompletableDeferred<Unit>()
                        wdRemoteEnd.onSessionReturnedEventCallbacks.add(onReturned)
                        onReturned.await()
                    }
                }
            }
            jobReference.set(job)
        }
    }

    object WebDriverHealthChecker {
        fun checkAvailability(webDriverCommandExecutor: info.vividcode.wd.WebDriverCommandExecutor, session: WebDriverSession): Boolean =
            with (webDriverCommandExecutor) {
                WebDriverCommand.Go(session, htmlDataUrl).execute()
                val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session, Script(js, listOf())).execute()
                (rawExecuteResult as? ScriptResult.String)?.value == "Health check"
            }

        private val htmlDataUrl = createHtmlDataUrl(
                "<!DOCTYP html><html><head><title>Health check</title></head><body></body></html>")
        private val js = """
            return document.title;
        """.trimIndent()
    }

    private class WdRemoteEnd(
            val webDriverCommandExecutor: info.vividcode.wd.WebDriverCommandExecutor,
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

    private class WdSessionInfo(
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

}
