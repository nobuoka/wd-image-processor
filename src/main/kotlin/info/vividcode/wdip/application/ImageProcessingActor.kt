package info.vividcode.wdip.application

import kotlinx.coroutines.experimental.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ImageProcessingActor() {

    private var job: Job? = null

    private val cancelRequested = AtomicBoolean(false)

    fun start(queue: ImageProcessingRequestQueue<String, String>): Unit = synchronized(this) {
        if (job != null) {
            throw IllegalStateException("Already Started")
        }
        cancelRequested.set(false)
        job = launch {
            try {
                while (!cancelRequested.get()) {
                    queue.receiveImageProcessingRequest() {
                        handleRequest(it)
                    }
                }
            } finally {
                synchronized(this@ImageProcessingActor) {
                    job = null
                }
            }
        }
    }

    fun stop(): Deferred<Unit> = synchronized(this) {
        cancelRequested.set(true)
        val j = job
        return@synchronized async<Unit> { j?.join() }
    }

    private suspend fun handleRequest(request: String): String {
        delay(4, TimeUnit.SECONDS)
        return "OK! [$request]"
    }

}
