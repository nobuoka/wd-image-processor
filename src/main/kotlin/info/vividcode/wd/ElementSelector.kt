package info.vividcode.wd

data class ElementSelector(val strategy: Strategy, val value: String) {
    enum class Strategy {
        //CSS, // ChromeDriver は "css" で、geckodriver は "css selector"?
        XPATH,
    }
}
