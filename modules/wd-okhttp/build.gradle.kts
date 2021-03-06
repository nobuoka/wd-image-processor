import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.2"
}

dependencies {
    val okHttpVersion = "3.9.1"
    val junitJupiterVersion = "5.2.0"

    implementation(kotlin("stdlib-jdk8"))
    api(project(":modules:wd"))
    api("com.squareup.okhttp3:okhttp:$okHttpVersion")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testRuntimeOnly("org.glassfish:jakarta.json:1.1.5")

    testImplementation(project(":modules:test-utils"))

    testImplementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val test by tasks.existing(Test::class) {
    useJUnitPlatform()
}

val jacocoTestReport by tasks.existing(JacocoReport::class) {
    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
        html.destination = file("$buildDir/reports/jacoco/html")
    }
}
