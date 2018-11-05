import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    application
    kotlin("jvm") version "1.2.71"
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
        maven { url = URI("http://dl.bintray.com/kotlin/ktor") }
        maven { url = URI("https://dl.bintray.com/kotlin/kotlinx") }
    }
}

dependencies {
    val okHttpVersion = "3.9.1"
    val ktorVersion = "0.9.5"
    val junitJupiterVersion = "5.2.0"

    implementation(project(":modules:wd"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okHttpVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    // JUnit Jupiter API and TestEngine implementation
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("com.squareup.okhttp3:mockwebserver:$okHttpVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
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

val jacocoMerge by tasks.registering(JacocoMerge::class) {
    gradle.afterProject {
        val p = this
        if (p.plugins.hasPlugin("jacoco")) {
            val testTask = p.tasks.withType(Test::class.java)["test"]!!
            executionData((testTask.extensions["jacoco"] as JacocoTaskExtension).destinationFile)
            dependsOn(testTask)
        }
    }
}

val jacocoMergedReport by tasks.registering(JacocoReport::class) {
    dependsOn(jacocoMerge)
    executionData(jacocoMerge.get().destinationFile)
    sourceDirectories = files()
    classDirectories = files()
    gradle.afterProject {
        val p = this
        if (p.plugins.hasPlugin("java")) {
            val sourceSets = (p.convention.getPlugin(JavaPluginConvention::class.java)).sourceSets
            sourceDirectories += files(listOf(sourceSets["main"].allJava.srcDirs))
            classDirectories += sourceSets["main"].output
        }
    }

    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
        html.destination = file("$buildDir/reports/jacoco/html")
    }
}

val check by tasks.existing {
    dependsOn(jacocoMergedReport)
}
