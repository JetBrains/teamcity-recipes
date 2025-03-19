import java.net.URL

plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.jetbrains.teamcity"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("software.amazon.awssdk:s3:2.30.38")
    testImplementation("software.amazon.awssdk:auth:2.30.38")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")

    testImplementation("org.testcontainers:minio:1.20.6")
}

val kotlinVersionForTests = "2.1.10"
val kotlinUrl =
    "https://github.com/JetBrains/kotlin/releases/download/v$kotlinVersionForTests/kotlin-compiler-$kotlinVersionForTests.zip"
val kotlinDir = File(layout.buildDirectory.get().asFile.absolutePath, "kotlin-compiler")

tasks.register("downloadKotlinCompilerForTests") {
    doLast {
        if (kotlinDir.exists()) kotlinDir.deleteRecursively()
        println("Downloading Kotlin Compiler $kotlinVersionForTests...")
        val zipFile = File(layout.buildDirectory.get().asFile.absolutePath, "kotlin-compiler.zip")
        URL(kotlinUrl).openStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        copy {
            from(zipTree(zipFile))
            into(kotlinDir)
        }
        zipFile.delete()
        println("Extracted Kotlin Compiler to ${kotlinDir.absolutePath}")
    }
}

tasks.named<Test>("test") {
    dependsOn("downloadKotlinCompilerForTests")
    val separator = if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
    environment("PATH", "${kotlinDir.absolutePath}/bin$separator${System.getenv("PATH")}")
    useJUnitPlatform()
}