import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.1.0")
    }
}

plugins {
    application
    kotlin("jvm") version "1.2.41"
}
apply { plugin("org.junit.platform.gradle.plugin") }

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
    val ktorVersion = "0.9.2"
    val junitJupiterVersion = "5.1.0"

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("com.beust:klaxon:2.1.4")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // JUnit Jupiter API and TestEngine implementation
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}
