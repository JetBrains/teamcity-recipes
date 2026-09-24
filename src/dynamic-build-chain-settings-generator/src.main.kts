@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jetbrains.teamcity:common:2026.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.SystemInfo
import jetbrains.buildServer.util.http.HttpMethod
import jetbrains.buildServer.util.retry.AbortRetriesException
import jetbrains.buildServer.util.retry.Retrier
import jetbrains.buildServer.util.retry.RetrierEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class HttpErrorException(val statusCode: Int, message: String) : Exception("Failed to execute HTTP request: $statusCode - $message")

val retrier = Retrier.withAttempts(3, Retrier.DelayStrategy.exponentialBackoff(1000))
    .registerListener(HttpErrorCodeListener())

val AGENT_TOKEN = "agent_token"

runCatchingWithLogging {
    val settingsDirectory = requiredInput("settings_directory")
    val serverUrl = requiredInput("server_url")
    val projectId = requiredInput("project_id")
    val accessToken = requiredInput(AGENT_TOKEN)
    val mavenToolPath = requiredInput("maven_tool_path")

    val settingsDirectoryFile = File(settingsDirectory)

    if (!settingsDirectoryFile.exists() || !settingsDirectoryFile.isDirectory) {
        throw RuntimeException("Versioned settings directory does not exist or is not a directory.")
    }

    fun generateSettings() {
        val httpRequest = HTTPRequestBuilder("$serverUrl/app/dsl-context?projectExtId=$projectId")
            .withHeader("Authorization", "Bearer $accessToken")
            .withMethod(HttpMethod.GET)
            .allowNonSecureConnection(true)
            .onErrorResponse { statusCode, message -> throw HttpErrorException(statusCode, message) }
            .build()

        val requestHandler = HTTPRequestBuilder.ApacheClient43RequestHandler()
        val response = retrier.execute<HTTPRequestBuilder.Response> {
            requestHandler.doSyncRequest(httpRequest)
        }

        response.use {
            val dslContext = it.contentStream ?: throw RuntimeException("Failed to retrieve DSL context from the server.")
            createBinaryFile("$settingsDirectory/dsl-context.zip", dslContext)
        }

        val mavenPath = if (SystemInfo.isWindows){
            "$mavenToolPath\\bin\\mvn.cmd"
        } else {
            "$mavenToolPath/bin/mvn"
        }

        val result = ProcessUtils.runProcess(
            listOf(
                mavenPath,
                "-Dteamcity.versionedSettings.exposeInternalParameters=true",
                "-Dteamcity.internal.dsl.IS_DYNAMIC_CHAIN=true",
                "-DserverContext=dsl-context.zip",
                "teamcity-configs:generate",
                "-f", "pom.xml",
            ),
            settingsDirectoryFile,
            removeEnv = listOf("input_$AGENT_TOKEN")
        )
        if (result == null || result.exitCode != 0) {
            throw RuntimeException("Settings generation failed" + (result?.let { " with exit code ${it.exitCode}" } ?: ""))
        }

        val generatedParentProject = File(settingsDirectoryFile, "target/generated-configs/$projectId")
        if (!generatedParentProject.exists()) {
            throw RuntimeException("Generation doesn't match our expected structure")
        }

        generatedParentProject.deleteRecursively()
    }

    fun generateDslParamsFile() {
        val dslParamsFile = File(settingsDirectoryFile, "params.properties")
        val params = StringUtil.emptyIfNull(System.getenv("input_params"))

        dslParamsFile.writer().use { writer ->
            writer.append("IS_DYNAMIC_CHAIN=true\n")
            params.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                val parts = it.split("=", limit = 2)
                if (parts.size != 2) {
                    throw RuntimeException("Invalid DSL parameter '$it', expected 'name=value'.")
                }
                writer.append("${parts[0].trim()}=${parts[1].trim()}\n")
            }
        }
    }

    generateDslParamsFile()
    generateSettings()
}

class HttpErrorCodeListener : RetrierEventListener {

    override fun <T> onFailure(callable: Callable<T?>, attempt: Int, e: java.lang.Exception) {
        if ((e is HttpErrorException && e.statusCode >= 500) || e is IOException) {
            return
        }

        throw AbortRetriesException(e)
    }
}

object ProcessUtils {
    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    data class RunOptions(
        val isSilent: Boolean = false,
        val executionTimeout: Duration = 30.minutes,
    )

    fun runProcess(
        command: List<String>,
        workingDir: File,
        options: RunOptions = RunOptions(),
        removeEnv: List<String> = emptyList()
    ): ProcessResult? = runBlocking {
        if (!options.isSilent) {
            println("Starting: ${command.joinToString(" ")}")
            println("In directory: ${workingDir.absolutePath}")
        }
        try {
            val processBuilder = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(false)
            modifyProcessEnvironment(processBuilder, removeEnv)

            val process = processBuilder.start()

            val stdoutDeferred = readLines(process.inputStream, options.isSilent, false)
            val stderrDeferred = readLines(process.errorStream, options.isSilent, true)

            if (!process.waitFor(options.executionTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                if (!options.isSilent) {
                    System.err.println("Execution timeout exceeded")
                }
                process.destroy()
                process.waitFor(5, TimeUnit.SECONDS)
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                return@runBlocking null
            }
            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()

            ProcessResult(process.exitValue(), stdout, stderr)
        } catch (e: Throwable) {
            if (!options.isSilent) {
                System.err.println("Failed to execute command")
                System.err.println(e.stackTraceToString())
            }
            return@runBlocking null
        }
    }

    fun modifyProcessEnvironment(processBuilder: ProcessBuilder, removeEnv: List<String>) {
        val envVariables = processBuilder.environment()
        removeEnv.forEach { envVariables.remove(it) }
    }

    private fun CoroutineScope.readLines(inputStream: InputStream, isSilent: Boolean, isError: Boolean) =
        async(Dispatchers.IO) {
            val lines = mutableListOf<String>()
            inputStream.bufferedReader().forEachLine { line ->
                lines.add(line)
                if (!isSilent) {
                    if (isError) System.err.println(line) else println(line)
                }
            }
            lines.joinToString(System.lineSeparator())
        }
}

fun createBinaryFile(filePath: String, content: InputStream) = File(filePath).run {
    parentFile?.mkdirs() // Ensure directories exist
    outputStream().use { content.copyTo(it) }
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