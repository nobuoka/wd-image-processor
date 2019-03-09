package info.vividcode.wdip

data class ApplicationEnvironmentVariables(
        val webDriverBaseUrls: List<String>,
        val webDriverSessionCapacity: Int,
        val processorsConfigPath: String
) {

    companion object {
        fun load(getenv: (String) -> String?): ApplicationEnvironmentVariables {
            val webDriverBaseUrls = (getenv("WD_BASE_URLS") ?: "http://localhost:10001").split(Regex("\\s"))
            // Session of WebDriver will be recreated after requests are received `WD_SESSION_CAPACITY` times
            val webDriverSessionCapacity = getenv("WD_SESSION_CAPACITY")?.toIntOrNull() ?: 10
            val processorsConfigJsonPath = getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/config.json"

            return ApplicationEnvironmentVariables(
                    webDriverBaseUrls = webDriverBaseUrls,
                    webDriverSessionCapacity = webDriverSessionCapacity,
                    processorsConfigPath = processorsConfigJsonPath
            )
        }
    }

}
