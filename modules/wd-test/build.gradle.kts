import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    val kotlinxCoroutinesVersion = "1.0.0"
    val klaxonVersion = "2.1.4"
    val okHttpVersion = "3.9.1"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    implementation(project(":modules:wd"))

    implementation("com.beust:klaxon:$klaxonVersion")
    implementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
