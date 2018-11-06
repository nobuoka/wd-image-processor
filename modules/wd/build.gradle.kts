import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    val junitJupiterVersion = "5.2.0"

    implementation(kotlin("stdlib-jdk8"))
    api("com.beust:klaxon:2.1.4")

    // JUnit Jupiter API and TestEngine implementation
    testCompileOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val test = tasks.withType(Test::class.java)["test"]!!.apply {
    useJUnitPlatform()
}
