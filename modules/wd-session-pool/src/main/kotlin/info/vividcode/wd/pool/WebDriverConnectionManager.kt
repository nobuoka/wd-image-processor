package info.vividcode.wd.pool

import info.vividcode.wd.Timeouts
import info.vividcode.wd.WebDriverSession
import info.vividcode.wd.http.WebDriverCommandExecutor
import info.vividcode.wd.http.WebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.internal.WdRemoteEnd
import info.vividcode.wd.pool.internal.WdRemoteEndManagingActor
import info.vividcode.wd.pool.internal.WdSessionInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

}
