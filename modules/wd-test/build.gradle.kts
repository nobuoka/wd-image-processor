import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import info.vividcode.wd.gradle.Versions

plugins {
    kotlin("jvm")
    `java-library`
}

val versions: Versions by rootProject.extensions

dependencies {
    val kotlinxCoroutinesVersion = "1.0.0"
    val okHttpVersion = "3.9.1"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    implementation(project(":modules:wd"))

    implementation("com.beust:klaxon:${versions.klaxon}")
    implementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
