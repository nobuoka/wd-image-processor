import info.vividcode.wd.gradle.Versions
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

val versions = Versions(
        klaxon = "5.0.5"
).also { rootProject.extensions.add("versions", it) }

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

    // JUnit Jupiter API and TestEngine implementation
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

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

val jacocoMerge by tasks.registering(JacocoMerge::class)

val jacocoMergedReport by tasks.registering(JacocoReport::class) {
    dependsOn(jacocoMerge)
    executionData(jacocoMerge.get().destinationFile)

    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
        html.destination = file("$buildDir/reports/jacoco/html")
    }
}

val jacocoMergedReportSourceDirectories = mutableSetOf<File>()
val jacocoMergedReportClassDirectories = mutableSetOf<SourceSetOutput>()
gradle.afterProject {
    val p = this
    if (p.plugins.hasPlugin("jacoco")) {
        val jacocoMergeTask = jacocoMerge.get()
        val testTask = p.tasks.withType(Test::class.java)["test"]!!
        jacocoMergeTask.executionData((testTask.extensions["jacoco"] as JacocoTaskExtension).destinationFile)
        jacocoMergeTask.dependsOn(testTask)
    }
    if (p.plugins.hasPlugin("java")) {
        val jacocoMergedReportTask = jacocoMergedReport.get()
        val sourceSets = (p.convention.getPlugin(JavaPluginConvention::class.java)).sourceSets
        jacocoMergedReportSourceDirectories.addAll(sourceSets["main"].allJava.srcDirs)
        jacocoMergedReportClassDirectories.add(sourceSets["main"].output)
        jacocoMergedReportTask.setSourceDirectories(files(jacocoMergedReportSourceDirectories))
        jacocoMergedReportTask.setClassDirectories(files(jacocoMergedReportClassDirectories))
    }
}

val check by tasks.existing {
    dependsOn(jacocoMergedReport)
}
