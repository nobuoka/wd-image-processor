package info.vividcode.wdip.application

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

class RequestQueue<T, R> {

    private val count = AtomicInteger(0)

    private val channel: Channel<Pair<T, CompletableDeferred<R>>> = Channel(128)

    suspend fun requestImageProcessingRequest(request: T): R {
        count.incrementAndGet()
        val d = CompletableDeferred<R>()
        channel.send(Pair(request, d))
        return try {
            d.await()
        } finally {
            count.decrementAndGet()
        }
    }

    suspend fun receiveImageProcessingRequest(handler: suspend (T) -> R) {
        val request = channel.receive()
        val d = request.second
        try {
            val result = handler(request.first)
            d.complete(result)
        } catch (e: Throwable) {
            d.completeExceptionally(e)
        }
    }

}
