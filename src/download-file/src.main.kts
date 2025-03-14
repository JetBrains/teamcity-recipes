@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.apache.httpcomponents.client5:httpclient5:5.4.2")
@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.HttpHeaders
import java.io.File
import java.io.FileOutputStream

runCatchingWithLogging {
    val url = requiredInput("url")
    val targetDirectory = requiredInput("output_dir")

    val targetFileName = input("filename")
    val bearerToken = input("bearer_token")
    val headers = input("headers")
        .lines().filter { it.isNotBlank() }

    val targetFile = getTargetFile(url, targetDirectory, targetFileName)
    downloadFile(url, targetFile, bearerToken, headers)

    println("File downloaded to: ${targetFile.absolutePath}")
}

fun getTargetFile(url: String, outputDir: String, targetFileName: String): File {
    val targetDir = outputDir
        .ifBlank { File(".").absolutePath }
        .let { File(it) }
        .normalize()

    val filePath = targetDir.let {
        when {
            it.isDirectory -> it
            !it.exists() -> {
                if (!it.mkdirs()) throw IllegalArgumentException("Failed to create directory: ${it.absolutePath}")
                it
            }

            else -> throw IllegalArgumentException("Destination already exists and is not a directory: ${it.absolutePath}")
        }
    }

    val fileName = sequenceOf(
        targetFileName,
        url.substringAfterLast('/'),
        "downloaded_file"
    ).first { it.isNotEmpty() }

    return File(filePath, fileName).normalize()
}

fun downloadFile(
    url: String,
    targetFile: File,
    authToken: String,
    customHeaders: List<String>
) {
    HttpClients.createDefault().use { httpClient ->
        val httpGet = HttpGet(url)

        if (authToken.isNotEmpty()) httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $authToken")

        customHeaders.forEach {
            if (!it.contains(":")) {
                error("Invalid header format: '$it'. Expected format: 'Header-Name: Value'")
            }
            val (name, value) = it.split(":", limit = 2).map(String::trim)
            httpGet.addHeader(name, value)
        }

        httpClient.execute(httpGet) { response ->
            val statusCode = response.code
            if (statusCode !in 200..299) {
                error("Failed to download file. HTTP Response: $response")
            }
            response.entity?.content?.use { inputStream ->
                FileOutputStream(targetFile).use { it.write(inputStream.readBytes()) }
            }
        }
    }
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")
fun input(name: String) = System.getenv("input_$name") ?: ""

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
        println(asString(MESSAGE, mapOf("text" to text, *attributes)))

    fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
    fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

    writeError("$it (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    kotlin.system.exitProcess(1)
}