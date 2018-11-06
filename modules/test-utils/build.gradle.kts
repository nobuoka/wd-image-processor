import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    val okHttpVersion = "3.9.1"
    val junitJupiterVersion = "5.2.0"

    implementation(kotlin("stdlib-jdk8"))

    compileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    api("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
