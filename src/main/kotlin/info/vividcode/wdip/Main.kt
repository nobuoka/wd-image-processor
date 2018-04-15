@file:JvmName("Main")

package info.vividcode.wdip

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import info.vividcode.wd.*
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandExecutor
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import kotlinx.coroutines.experimental.*
import okhttp3.OkHttpClient
import java.nio.charset.StandardCharsets

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.coroutines.experimental.CoroutineContext

fun main(args: Array<String>) {
    startServer()
}

class WebDriverSessionManager(okHttpClient: OkHttpClient, webDriverBaseUrls: List<String>) {
    private val wdCommandExecutorQueue: BlockingQueue<Pair<WebDriverCommandExecutor, WebDriverSession?>> =
        ArrayBlockingQueue(webDriverBaseUrls.size)

    private val context: CoroutineContext = newFixedThreadPoolContext(webDriverBaseUrls.size, "WebDriverContext")

    init {
        wdCommandExecutorQueue.addAll(
            webDriverBaseUrls
                .map { OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, it) }
                .map { Pair(OkHttpWebDriverCommandExecutor(it), null) }
        )
    }

    suspend fun <T> executeInSession(proc: WebDriverCommandExecutor.(session: WebDriverSession) -> T): T =
            async(context) {
                val (wd, continuedSession) = wdCommandExecutorQueue.take()
                var sessionToNext: WebDriverSession? = null
                try {
                    return@async with(wd) {
                        val session = if (continuedSession != null) continuedSession else WebDriverCommand.NewSession().execute()
                        val result: T
                        try {
                            result = proc(session)
                            sessionToNext = session
                        } catch (e: Exception) {
                            WebDriverCommand.DeleteSession(session).execute()
                            throw e
                        }
                        return@with result
                    }
                } finally {
                    wdCommandExecutorQueue.put(Pair(wd, sessionToNext))
                }
            }.await()
}

data class ProcessorSetting(val path: String, val html: String, val js: String, val key: String?)

fun parseProcessorsConfigJson(jsonFile: Path): List<ProcessorSetting> {
    fun Path.readContent(): String = Files.readAllBytes(jsonFile.parent.resolve(this)).toString(StandardCharsets.UTF_8)

    val configObject = jsonFile.toFile().reader().use { Klaxon().parseJsonObject(it) }
    return configObject.entries.map { (path, config) ->
        val htmlString = (config as? JsonObject)?.string("html")?.let { Paths.get(it).readContent() }
        val jsString = (config as? JsonObject)?.string("js")?.let { Paths.get(it).readContent() }
        val key = (config as? JsonObject)?.string("key")
        ProcessorSetting(path, htmlString ?: "", jsString ?: "", key)
    }
}

class WdImageProcessingExecutor(
    private val htmlString: String,
    private val jsString: String,
    private val webDriverSessionManager: WebDriverSessionManager
) {
    suspend fun execute(jsArg: String): ByteArray {
        return webDriverSessionManager.executeInSession { session ->
            executeImageProcessorWithElementScreenshot(session, htmlString, jsString, jsArg)
        }
    }
}

data class ScreenshotRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class ImageProcessorScriptResponse(
    val targetElement: WebElement?
)
