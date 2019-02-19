import info.vividcode.wd.gradle.Versions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.2"
}

val versions: Versions by rootProject.extensions

dependencies {
    val okHttpVersion = "3.9.1"
    val junitJupiterVersion = "5.2.0"

    implementation(kotlin("stdlib-jdk8"))
    api("com.beust:klaxon:${versions.klaxon}")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation(project(":modules:test-utils"))
    testImplementation(project(":modules:wd-okhttp"))

    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val test = tasks.withType(Test::class.java)["test"]!!.apply {
    useJUnitPlatform()
}
