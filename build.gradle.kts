import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    application
    kotlin("jvm") version "1.2.31"
}

group = "info.vividcode.example"
version = "1.0-SNAPSHOT"

application {
    applicationName = "wdip"
    mainClassName = "info.vividcode.wdip.Main"
}

repositories {
    jcenter()
    maven { url = URI("http://dl.bintray.com/kotlin/ktor") }
    maven { url = URI("https://dl.bintray.com/kotlin/kotlinx") }
}

dependencies {
    val okHttpVersion = "3.9.1"
    val ktorVersion = "0.9.1"

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("com.beust:klaxon:2.1.4")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}
