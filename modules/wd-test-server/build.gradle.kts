import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    val okHttpVersion = "3.9.1"

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.glassfish:javax.json:1.1.4")
    implementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
