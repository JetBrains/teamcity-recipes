@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import java.io.File
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE

runCatchingWithLogging {
    createTextFile(requiredInput("path"), requiredInput("content"))
}

fun createTextFile(filePath: String, content: String) = File(filePath).run {
    parentFile?.mkdirs() // Ensure directories exist
    writeText(content)
    println("File created successfully: $absolutePath")
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
        println(asString(MESSAGE, mapOf("text" to text, *attributes)))

    fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
    fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

    writeError("$it (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    kotlin.system.exitProcess(1)
}