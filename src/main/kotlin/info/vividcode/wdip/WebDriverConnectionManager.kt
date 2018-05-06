package info.vividcode.wdip

import info.vividcode.wd.*
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandExecutor
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.selects.select
import okhttp3.OkHttpClient
import kotlin.coroutines.experimental.CoroutineContext

class WebDriverConnectionManager(okHttpClient: OkHttpClient, webDriverBaseUrls: Collection<String>) {

    private val wdRemoteEndManagingActors: Set<WdRemoteEndManagingActor>

    private val webDriverManagerContext: CoroutineContext = newSingleThreadContext("WebDriverManagerContext")
    private val webDriverExecutionContext: CoroutineContext =
        newFixedThreadPoolContext(webDriverBaseUrls.size, "WebDriverConnectionContext")

    private val sessionRequestChannel = Channel<CompletableDeferred<WdSessionInfo>>(100)

    init {
        val wdCommandExecutors = webDriverBaseUrls
            .map { OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, it) }
            .map { OkHttpWebDriverCommandExecutor(it) }
        wdRemoteEndManagingActors = wdCommandExecutors
            .map { WdRemoteEnd(it, webDriverExecutionContext) }
            .map { WdRemoteEndManagingActor(sessionRequestChannel, it) }.toSet()
        wdRemoteEndManagingActors.forEach { it.start(webDriverManagerContext) }
    }

    suspend fun checkAllWebDriverRemoteEndsAvailable(): List<Boolean> =
        wdRemoteEndManagingActors.map {
            async(webDriverManagerContext) { it.checkWebDriverRemoteEndAvailable() }
        }.map {
            it.await()
        }

    suspend fun <T> withSession(block: WebDriverCommandExecutor.(session: WebDriverSession) -> T): T =
        withContext(webDriverManagerContext) {
            val sessionDeferred = CompletableDeferred<WdSessionInfo>()
            sessionRequestChannel.send(sessionDeferred)
            val sessionInfo = sessionDeferred.await()
            try {
                async(webDriverExecutionContext) {
                    block(sessionInfo.correspondingWdRemoteEnd.webDriverCommandExecutor, sessionInfo.session)
                }.await()
            } finally {
                sessionInfo.finishUse()
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
            if (job == null) {
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
                                    val sessionInfo = wdRemoteEnd.publishSession()
                                    val result = try {
                                        withContext(sessionInfo.correspondingWdRemoteEnd.webDriverExecutionContext) {
                                            WebDriverHealthChecker.checkAvailability(
                                                sessionInfo.correspondingWdRemoteEnd.webDriverCommandExecutor,
                                                sessionInfo.session)
                                        }
                                    } finally {
                                        sessionInfo.finishUse()
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

        suspend fun returnSession(sessionInfo: WdSessionInfo) {
            val removed = sessionsInUse.remove(sessionInfo)
            if (!removed) {
                throw RuntimeException("Unknown session")
            }

            if (sessionInfo.numUsed < maxNumSessionUsed) {
                sessionsIdle.add(sessionInfo)
            } else {
                async(webDriverExecutionContext) {
                    with(webDriverCommandExecutor) {
                        WebDriverCommand.DeleteSession(sessionInfo.session).execute()
                    }
                }.await()
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

        suspend fun finishUse() {
            numUsed++
            correspondingWdRemoteEnd.returnSession(this)
        }
    }

}
