package info.vividcode.wdip.web

data class WdipSetting(
    val accessControlAllowOrigins: Set<String>,
    val processors: List<ProcessorSetting>
)