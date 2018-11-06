package info.vividcode.wdip.application

class WdImageProcessingResult(
        val statusCode: Int,
        val content: WdImageProcessingResultContent?,
        val httpCache: HttpCache?
)
