package info.vividcode.wdip

import info.vividcode.wd.*
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandExecutor
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.selects.select
import okhttp3.OkHttpClient
import kotlin.coroutines.experimental.CoroutineContext

class WebDriverConnectionManager(okHttpClient: OkHttpClient, webDriverBaseUrls: Collection<String>, webDriverSessionCapacity: Int) {

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
                            OkHttpWebDriverCommandExecutor(
                                OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, webDriverBaseUrl)
                            ),
                            webDriverExecutionContext,
                            maxNumSessionUsed = webDriverSessionCapacity
                        )
                    )
        }.toMap()
        wdRemoteEndManagingActorMap.values.forEach { it.start(webDriverManagerContext) }
    }

    suspend fun checkAllWebDriverRemoteEndsAvailable(): List<Boolean> =
        wdRemoteEndManagingActorMap.values.map {
            async(webDriverManagerContext) { it.checkWebDriverRemoteEndAvailable() }
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
        val r = async(webDriverManagerContext) { v.checkWebDriverRemoteEndAvailable() }
        return try {
            r.await()
        } catch (rawException: Exception) {
            val e = RuntimeException(rawException)
            e.printStackTrace()
            false
        }
    }

    suspend fun <T> withSession(block: WebDriverCommandExecutor.(session: WebDriverSession) -> T): T =
        withContext(webDriverManagerContext) {
            val sessionDeferred = CompletableDeferred<WdSessionInfo>()
            sessionRequestChannel.send(sessionDeferred)
            sessionDeferred.await().use { sessionInfo ->
                async(webDriverExecutionContext) {
                    block(sessionInfo.correspondingWdRemoteEnd.webDriverCommandExecutor, sessionInfo.session)
                }.await()
            }
        }

    private class WdRemoteEndManagingActor(
        private val sessionRequestChannel: Channel<CompletableDeferred<WdSessionInfo>>,
        private val wdRemoteEnd: WdRemoteEnd
    ) {
        private var job: Job? = null
        private val healthCheckRequestChannel = Channel<CompletableDeferred<Boolean>>(10)

        /** Must be run on managerContext. */
        suspend fun checkWebDriverRemoteEndAvailable(): Boolean =
            if (job?.isActive != true) {
                false
            } else {
                val resultDeferred = CompletableDeferred<Boolean>()
                healthCheckRequestChannel.send(resultDeferred)
                resultDeferred.await()
            }

        fun start(managerContext: CoroutineContext) = synchronized(this) {
            job = launch(managerContext) {
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
        }
    }

    object WebDriverHealthChecker {
        fun checkAvailability(webDriverCommandExecutor: WebDriverCommandExecutor, session: WebDriverSession): Boolean =
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
        val webDriverCommandExecutor: OkHttpWebDriverCommandExecutor,
        val webDriverExecutionContext: CoroutineContext,
        val sessionsIdle: MutableSet<WdSessionInfo> = mutableSetOf(),
        val sessionsInUse: MutableSet<WdSessionInfo> = mutableSetOf(),
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
                val wdSession = async(webDriverExecutionContext) {
                    with(webDriverCommandExecutor) {
                        WebDriverCommand.NewSession().execute()
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
                async(webDriverExecutionContext) {
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
            var errorOccurred = false
            try {
                return block(this)
            } catch (e: Exception) {
                errorOccurred = true
                throw e
            } finally {
                finishUse(errorOccurred)
            }
        }

        suspend fun finishUse(errorOccurred: Boolean) {
            try {
                numUsed++
                correspondingWdRemoteEnd.returnSession(this, errorOccurred)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
