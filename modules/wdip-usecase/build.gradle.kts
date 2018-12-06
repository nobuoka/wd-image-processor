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
    val kotlinxCoroutinesVersion = "1.0.0"
    val klaxonVersion = "2.1.4"
    val junitJupiterVersion = "5.2.0"
    val okHttpVersion = "3.9.1"

    implementation(project(":modules:wd"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("com.beust:klaxon:$klaxonVersion")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation(project(":modules:wd-okhttp"))
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}
