@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.apache.commons:commons-lang3:3.17.0")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.15.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_SET_PARAMETER
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.SystemUtils
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.*
import kotlin.math.min

runCatchingWithLogging {
    val version = requiredInput("version")
    val installationPath = input("installation_path")
        .ifBlank { "." }
        .let { Path(it).toAbsolutePath().normalize() }
        .getOrTemp()

    runBlocking {
        installBazelisk(version, installationPath, OS.detect(), Arch.detect())
    }
}

suspend fun installBazelisk(version: String, installationPath: Path, os: OS, arch: Arch) {
    val bazeliskVersion = BazeliskVersionManager.tryGetBazeliskVersion(version, os, arch)

    val installDirectory = installationPath.resolve("bazelisk-${bazeliskVersion.version}-${os}-${arch}")
    val executableName = "bazel" + if (os == OS.Windows) ".exe" else ""
    val installPath = installDirectory.resolve(executableName)
    val tempFile = installDirectory.resolve("${executableName}.tmp")

    if (installPath.exists()) {
        println("Bazelisk ${bazeliskVersion.version} is already installed to '$installDirectory'")
        addToPath(installDirectory.absolutePathString())
        return
    }

    println("Downloading ${bazeliskVersion.downloadUrl} to $installPath")
    try {
        installDirectory.createDirectories()

        retry { bazeliskVersion.downloadUrl.downloadTo(tempFile) }
        println("Downloaded to $tempFile")

        tempFile.toFile().setExecutable(true)
        println("Set binary executable flag")

        move(tempFile, installPath)
        println("Moved to $installDirectory")
    } finally {
        tempFile.deleteIfExists()
    }

    addToPath(installDirectory.absolutePathString())
}

object BazeliskVersionManager {
    private val apiUrl = "https://api.github.com/repos/bazelbuild/bazelisk/releases?per_page=100&page=1"

    suspend fun tryGetBazeliskVersion(versionPrefix: String, os: OS, arch: Arch): BazeliskVersionDownloadInfo {
        val json = Retry().retry { downloadJson(apiUrl) }
        val releases = parseJson(json)

        val release = releases.find { it.tagName.startsWith("v$versionPrefix") }
            ?: error(
                "Could not find the '$versionPrefix' release. Available: " +
                        releases.joinToString { it.tagName.trimStart('v') })

        val archName = when (arch) {
            Arch.X64 -> "amd64"
            Arch.ARM64 -> "arm64"
            else -> error("Unsupported architecture: '$arch'")
        }
        val expectedName = when (os) {
            OS.Linux -> "bazelisk-linux-$archName"
            OS.Mac -> "bazelisk-darwin-$archName"
            OS.Windows -> "bazelisk-windows-$archName.exe"
        }

        val asset = release.assets.find { it.name == expectedName }
            ?: error("Could not find asset matching '$expectedName'. Available: '${release.assets.joinToString { it.name }}'")

        return BazeliskVersionDownloadInfo(
            release.tagName,
            expectedName,
            URI.create(asset.browserDownloadUrl).toURL()
        )
    }

    private suspend fun downloadJson(url: String): String = withContext(Dispatchers.IO) {
        println("Fetching Bazelisk releases from $url")
        try {
            URL(url).openConnection().inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch Bazelisk releases from $url", e)
        }
    }

    private fun parseJson(json: String): List<BazeliskRelease> {
        val mapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(json)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class BazeliskRelease(
        @param:JsonProperty("tag_name")
        val tagName: String,
        val assets: List<BazeliskAsset>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class BazeliskAsset(
        val name: String,
        @param:JsonProperty("browser_download_url")
        val browserDownloadUrl: String
    )
}

data class BazeliskVersionDownloadInfo(
    val version: String,
    val filename: String,
    val downloadUrl: URL
)

private fun URL.downloadTo(destination: Path) {
    try {
        this.openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to download Bazelisk from $this to $destination", e)
    }
}

private fun move(from: Path, to: Path) {
    try {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
    }
}

enum class OS(val value: String) {
    Windows("win"),
    Linux("linux"),
    Mac("darwin");

    override fun toString() = value

    companion object {
        fun detect(): OS = when {
            SystemUtils.IS_OS_WINDOWS -> Windows
            SystemUtils.IS_OS_LINUX -> Linux
            SystemUtils.IS_OS_MAC -> Mac
            else -> error("Unsupported OS")
        }
    }
}

enum class Arch(val value: String) {
    X86("x86"),
    X64("x64"),
    ARM64("arm64");

    override fun toString() = value

    companion object {
        fun detect(): Arch = when (val arch = SystemUtils.OS_ARCH.lowercase()) {
            "x86" -> X86
            "amd64", "x86_64" -> if (SystemUtils.IS_OS_MAC && isProcTranslated()) ARM64 else X64
            "aarch64", "arm64" -> ARM64
            else -> error("Unsupported architecture: '$arch'")
        }

        private fun isProcTranslated() = runCatching {
            Runtime.getRuntime().exec(arrayOf("sysctl", "-n", "sysctl.proc_translated"))
                .inputStream.bufferedReader().use { it.readLine() == "1" }
        }.getOrDefault(false)
    }
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")
fun input(name: String) = System.getenv("input_$name") ?: ""

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    val errorMessage = it.message ?: it.javaClass.name
    writeError("$errorMessage (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    kotlin.system.exitProcess(1)
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

internal suspend fun <T> retry(body: suspend () -> T) = Retry().retry(body)

private class Retry {
    private companion object {
        val BACKOFF_LIMIT_MS = TimeUnit.MINUTES.toMillis(1)
        val INITIAL_DELAY_MS = TimeUnit.SECONDS.toMillis(10)
        const val MAX_ATTEMPTS = 3
        const val BACKOFF_FACTOR = 2
        const val BACKOFF_JITTER = 0.1
    }

    private val random by lazy(::Random)

    @OptIn(ExperimentalContracts::class)
    suspend fun <T> retry(body: suspend () -> T): T {
        contract {
            callsInPlace(body, InvocationKind.AT_LEAST_ONCE)
        }

        var effectiveDelay = INITIAL_DELAY_MS
        for (i in 1..MAX_ATTEMPTS) try {
            return body()
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            if (i == MAX_ATTEMPTS) {
                error("'${e.toString()} (Failed all $MAX_ATTEMPTS attempts)'")
            }
            if (i > 1) {
                effectiveDelay = backOff(effectiveDelay, e)
            }
            println("Retry $i of $MAX_ATTEMPTS failed with '${e.toString()}'. Retrying in ${effectiveDelay}ms")
            if (effectiveDelay > 0) {
                delay(effectiveDelay)
            }
        }
        error("Should not be reached")
    }

    private fun backOff(previousDelay: Long, cause: Throwable): Long {
        val nextDelay = min(previousDelay * BACKOFF_FACTOR, BACKOFF_LIMIT_MS) +
                (random.nextGaussian() * previousDelay * BACKOFF_JITTER).toLong()
        if (nextDelay > BACKOFF_LIMIT_MS) {
            error("Back off limit ${BACKOFF_LIMIT_MS}ms exceeded: ${cause.toString()}")
        }
        return nextDelay
    }
}

fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
fun writeWarning(text: String) = writeMessage(text, "status" to "WARNING")
fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
    println(asString(MESSAGE, mapOf("text" to text, *attributes)))

fun Path.getOrTemp(): Path {
    if (Files.notExists(this)) {
        try {
            Files.createDirectories(this)
        } catch (e: IOException) {
            return fallbackWithWarning(this, e)
        }
    }
    if (Files.isDirectory(this)) {
        try {
            Files.createTempFile(this, "write-test-", ".tmp").let { testFile ->
                Files.deleteIfExists(testFile)
            }
            return this
        } catch (e: IOException) {
            return fallbackWithWarning(this, e)
        }
    }
    return fallbackWithWarning(this, IOException("Not a directory"))
}

fun fallbackWithWarning(path: Path, cause: Exception): Path {
    val tmpDir = System.getProperty("java.io.tmpdir")
        ?: throw IllegalStateException("No 'java.io.tmpdir' defined", cause)
    writeWarning("Installation path '$path' is not writable, using '$tmpDir' (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(cause.stackTraceToString())
    return Paths.get(tmpDir)
}