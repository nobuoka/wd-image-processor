import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
}
apply { plugin("jacoco") }

configure<JacocoPluginExtension> {
    toolVersion = "0.8.2"
}

dependencies {
    val okHttpVersion = "3.9.1"
    val junitJupiterVersion = "5.2.0"

    implementation(kotlin("stdlib-jdk8"))
    api("com.squareup.okhttp3:okhttp:$okHttpVersion")
    api("com.beust:klaxon:2.1.4")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val test = tasks.withType(Test::class.java)["test"]!!.apply {
    useJUnitPlatform()
}
