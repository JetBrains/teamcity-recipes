@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2025.03")
@file:DependsOn("org.jetbrains.teamcity:common-api:2025.03")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.*
import jetbrains.buildServer.util.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.SystemUtils
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

val version: String = input("version")
    .removePrefix("v")
    .ifBlank { error("Dagger version is not set") }
val command: String = input("command")
val workdir: Path = input("workdir")
    .run { toNormalizedPath() }
val stopEngine: Boolean = input("stop_engine")
    .ifBlank { "true" }
    .toBoolean()
val installationPath: Path = input("installation_path")
    .run { toNormalizedPath() }
    .getOrTemp()
    .run { resolve("dagger-$version") }
val cloudToken: String = input("cloud_token")

runCatchingWithLogging {
    val dagger = daggerExecutablePath()
    when {
        dagger.isRegularFile() -> println("Dagger $version is already installed at $dagger")
        else -> installDagger()
    }
    addToPath(installationPath.absolutePathString())
    val exitCode = if (command.isNotBlank()) runDaggerCommand() else 0
    if (stopEngine) {
        tryStopDaggerEngine()
    }
    exitProcess(exitCode)
}

fun daggerExecutablePath(): Path =
    installationPath.resolve(if (SystemUtils.IS_OS_WINDOWS) "dagger.exe" else "dagger")

fun installDagger(): Unit = runWithin(logBlock = "Installing Dagger") {
    val path = installationPath.absolutePathString()
    println("Installing Dagger to $path")
    val installer = downloadAsTempFile("https://dl.dagger.io/dagger/install.sh")
        .also { it.toFile().setExecutable(true) }
    val command = buildList {
        if (SystemUtils.IS_OS_WINDOWS) add("bash")
        add(installer.absolutePathString())
    }
    runWithin(logBlock = "install.sh") {
        val result = Process.run(
            command = command,
            workingDir = installer.parent,
            environment = mapOf("BIN_DIR" to path, "DAGGER_VERSION" to version),
            listener = object : Process.Listener {
                override fun onStdout(line: String) = println(line)
                override fun onStderr(line: String) = println(line)
            }
        )
        when (result) {
            is Process.Result.Success -> Unit
            is Process.Result.Error.NonZeroCode -> exitProcess(result.exitCode)
            is Process.Result.Error.Exception -> throw result.reason
            else -> exitProcess(1)
        }
    }
}

fun runDaggerCommand(): Int = runWithin(logBlock = "Running Dagger command") {
    val environment = buildMap {
        if (cloudToken.isNotBlank()) {
            put("DAGGER_CLOUD_TOKEN", cloudToken)
        }
    }
    val cleaned = command.removePrefix("dagger").trimStart()
    val command = "${daggerExecutablePath().absolutePathString()} $cleaned"
    println("Running $command in directory: $workdir")
    val result = Process.run(
        command = StringUtil.splitCommandArgumentsAndUnquote(command),
        workingDir = workdir,
        environment = environment,
    )
    return@runWithin when (result) {
        is Process.Result.Success -> 0
        is Process.Result.Error.NonZeroCode -> result.exitCode
        is Process.Result.Error.Exception -> throw result.reason
        else -> 1
    }
}

fun tryStopDaggerEngine(): Unit = runWithin(logBlock = "Stopping Dagger engine") {
    val ps = listOf("docker", "ps", "--filter", "name=dagger-engine-*", "-q")
    println("Running: ${ps.joinToString(separator = " ")}")
    val psResult = Process.run(command = ps, workingDir = workdir)
    if (psResult !is Process.Result.Success) {
        System.err.println("Failed to list Dagger engine containers")
        return@runWithin
    }
    val containerIds: List<String> = psResult.stdout
        .split(System.lineSeparator())
        .filter { it.isNotBlank() }
    if (containerIds.isEmpty()) {
        println("Dagger engine containers not found")
    }
    containerIds.forEach { id ->
        val stop = listOf("docker", "stop", "-t", "300", id)
        println("Running: ${stop.joinToString(separator = " ")}")
        val stopResult = Process.run(command = stop, workingDir = workdir)
        if (stopResult !is Process.Result.Success) {
            System.err.println("Failed to stop container $id")
        } else println("Stopped $id")
    }
}

fun downloadAsTempFile(
    url: String,
    maxRetries: Int = 3,
    backoffMillis: Long = 1000L,
): Path {
    val tempFile = Files
        .createTempFile("", url.substringAfterLast("/"))
        .toAbsolutePath()
        .also { it.toFile().deleteOnExit() }
    for (attempt in 1..maxRetries) {
        println("Downloading $url to $tempFile")
        try {
            URI(url).toURL().openStream().use { input ->
                Files.newOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Throwable) {
            if (attempt == maxRetries) throw e
            val time = backoffMillis * attempt
            System.err.println("Download error: ${e.message}")
            System.err.println("Failed to download $url on $attempt attempt, will retry in ${time / 1000}s")
            Thread.sleep(time)
        }
    }
    return tempFile
}

private fun addToPath(newPath: String) {
    val updated = System.getenv("TEAMCITY_PATH_PREFIX").let {
        if (it.isNullOrBlank()) {
            newPath
        } else {
            "$newPath\n$it"
        }
    }

    val message = asString(
        BUILD_SET_PARAMETER, mapOf<String, String>("name" to "env.TEAMCITY_PATH_PREFIX", "value" to updated)
    )
    println(message)
}

object Process {
    sealed interface Result {
        data class Success(val stdout: String, val stderr: String) : Result

        sealed interface Error : Result {
            data class NonZeroCode(val exitCode: Int, val stdout: String, val stderr: String) : Error
            data class Exception(val reason: Throwable) : Error
            data object Timeout : Error
        }
    }

    data class Options(
        val executionTimeout: Duration = 5.hours,
    )

    interface Listener {
        fun onStdout(line: String)
        fun onStderr(line: String)
    }

    fun run(
        command: List<String>,
        workingDir: Path,
        environment: Map<String, String> = emptyMap(),
        options: Options = Options(),
        listener: Listener = object : Listener {
            override fun onStdout(line: String) = println(line)
            override fun onStderr(line: String) = System.err.println(line)
        },
    ): Result = runBlocking {
        try {
            val process = ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(false)
                .apply { environment().putAll(environment) }
                .start()
            val stdoutDeferred = readLines(process.inputStream, listener, false)
            val stderrDeferred = readLines(process.errorStream, listener, true)
            if (!process.waitFor(options.executionTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.destroy()
                process.waitFor(5, TimeUnit.SECONDS)
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                return@runBlocking Result.Error.Timeout
            }
            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            val code = process.exitValue()
            return@runBlocking if (code == 0)
                Result.Success(stdout, stderr)
            else Result.Error.NonZeroCode(code, stdout, stderr)
        } catch (e: Throwable) {
            return@runBlocking Result.Error.Exception(e)
        }
    }

    private fun CoroutineScope.readLines(inputStream: InputStream, listener: Listener, isError: Boolean) =
        async(Dispatchers.IO) {
            val lines = mutableListOf<String>()
            inputStream.bufferedReader().forEachLine { line ->
                lines.add(line)
                if (isError) listener.onStderr(line) else listener.onStdout(line)
            }
            lines.joinToString(System.lineSeparator())
        }
}

fun <T> runWithin(logBlock: String, block: () -> T): T {
    println(asString(BLOCK_OPENED, mapOf("name" to logBlock)))
    try {
        return block()
    } finally {
        println(asString(BLOCK_CLOSED, mapOf("name" to logBlock)))
    }
}

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    writeError("$it (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    exitProcess(1)
}

fun input(name: String) = System.getenv("input_$name") ?: ""
fun String.toNormalizedPath(): Path =
    ifBlank { "." }.let { Paths.get(it).toAbsolutePath().normalize() }

fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
fun writeWarning(text: String) = writeMessage(text, "status" to "WARNING")
fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
    println(asString(MESSAGE, mapOf("text" to text, *attributes)))

fun Path.getOrTemp(): Path {
    return when {
        !Files.exists(this) -> {
            try {
                Files.createDirectories(this)
                this
            } catch (e: Exception) {
                val temp = System.getProperty("java.io.tmpdir")
                writeWarning("Could not create directory at '$this', using '$temp' (Switch to 'Verbose' log level to see stacktrace)")
                writeDebug(e.stackTraceToString())
                Paths.get(temp)
            }
        }

        Files.isWritable(this) -> this

        else -> {
            val temp = System.getProperty("java.io.tmpdir")
            writeWarning("Installation path '$this' is not writable, using '$temp'")
            Paths.get(temp)
        }
    }
}