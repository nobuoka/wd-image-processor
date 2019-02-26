import org.ajoberstar.grgit.Grgit
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.0"
    id("org.ajoberstar.grgit") version "3.0.0-rc.2"
}
apply { plugin("jacoco") }

group = "info.vividcode.example"
version = "1.0-SNAPSHOT"

application {
    applicationName = "wdip"
    mainClassName = "info.vividcode.wdip.Main"
}

allprojects {
    repositories {
        jcenter()
    }
}

dependencies {
    val okHttpVersion = "3.9.1"
    val ktorVersion = "1.0.0"
    val junitJupiterVersion = "5.2.0"

    implementation(project(":modules:wd"))
    implementation(project(":modules:wd-okhttp"))
    implementation(project(":modules:wdip-usecase"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.glassfish:javax.json:1.1.4")

    // JUnit Jupiter API and TestEngine implementation
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.2"
}

val test by tasks.existing(Test::class) {
    useJUnitPlatform()
}

val grgit = project.ext["grgit"] as Grgit
val gitRevisionBuildDirPath = "$buildDir/generated/git"
val generateGitRevision by tasks.registering {
    val gitRevision = grgit.head().id
    inputs.property("git-revision", gitRevision)
    outputs.dir(gitRevisionBuildDirPath)

    doLast {
        val file = File(gitRevisionBuildDirPath, "wdip-git-revision")
        file.parentFile.mkdirs()
        file.writeText(gitRevision)
    }
}

val processResources by tasks.existing(ProcessResources::class) {
    dependsOn(generateGitRevision)
    from(gitRevisionBuildDirPath)
}

val jacocoTestReport by tasks.existing(JacocoReport::class) {
    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
        html.destination = file("$buildDir/reports/jacoco/html")
    }
}
