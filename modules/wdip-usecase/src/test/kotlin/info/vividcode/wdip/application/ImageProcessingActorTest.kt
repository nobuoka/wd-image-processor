package info.vividcode.wdip.application

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ImageProcessingActorTest {

    @Test
    fun test() {
        runBlocking {
            val queue = RequestQueue<String, String>()
            val actors = listOf(
                ImageProcessingActor(),
                ImageProcessingActor(),
                ImageProcessingActor()
            )

            actors.forEach { it.start(queue) }

            listOf(
                async {
                    val result = queue.requestImageProcessingRequest("test")
                    Assertions.assertEquals("OK! [test]", result)
                },
                async {
                    val result = queue.requestImageProcessingRequest("test")
                    Assertions.assertEquals("OK! [test]", result)
                },
                async {
                    val result = queue.requestImageProcessingRequest("test")
                    Assertions.assertEquals("OK! [test]", result)
                }
            ).forEach { it.await() }
        }
    }

    @Test
    internal fun stop() {
        runBlocking {
            val queue = RequestQueue<String, String>()
            val actor = ImageProcessingActor()

            actor.start(queue)

            run {
                val result = queue.requestImageProcessingRequest("test")
                Assertions.assertEquals("OK! [test]", result)
            }

            val deferred = actor.stop()

            // Actor will stop after last request is received.
            run {
                val result = queue.requestImageProcessingRequest("test")
                Assertions.assertEquals("OK! [test]", result)
            }

            deferred.await()
        }
    }

}
