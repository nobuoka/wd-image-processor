import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("jacoco")
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

jacoco {
    toolVersion = "0.8.2"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val test by tasks.existing(Test::class) {
    useJUnitPlatform()
}

dependencies {
    val okHttpVersion = "3.9.1"
    val kotlinxCoroutinesVersion = "0.25.0"
    val klaxonVersion = "2.1.4"
    val junitJupiterVersion = "5.2.0"

    implementation(project(":modules:wd"))
    implementation(project(":modules:wd-okhttp"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("com.beust:klaxon:$klaxonVersion")

    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}
