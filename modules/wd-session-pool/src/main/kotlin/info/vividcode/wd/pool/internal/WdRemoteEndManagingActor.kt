package info.vividcode.wd.pool.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

internal class WdRemoteEndManagingActor(
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
