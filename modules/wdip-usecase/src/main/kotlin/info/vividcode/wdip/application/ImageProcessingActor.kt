package info.vividcode.wdip.application

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ImageProcessingActor() {

    private var job: Job? = null

    private val cancelRequested = AtomicBoolean(false)

    fun start(queue: RequestQueue<String, String>): Unit = synchronized(this) {
        if (job != null) {
            throw IllegalStateException("Already Started")
        }
        cancelRequested.set(false)
        job = createJob(queue)
    }

    private fun createJob(queue: RequestQueue<String, String>): Job = GlobalScope.launch {
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

    fun stop(): Deferred<Unit> = run {
        val j = synchronized(this) {
            cancelRequested.set(true)
            job
        }
        return GlobalScope.async<Unit> { j?.join() }
    }

    private suspend fun handleRequest(request: String): String {
        delay(TimeUnit.SECONDS.toMillis(4))
        return "OK! [$request]"
    }

}
