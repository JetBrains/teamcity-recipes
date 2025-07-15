@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.15.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
@file:DependsOn("org.apache.commons:commons-compress:1.27.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_SET_PARAMETER
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FileUtils.copyURLToFile
import org.apache.commons.io.FileUtils.getTempDirectory
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

runCatchingWithLogging {
    val version = requiredInput("version")
    val installationPath = input("installation_path")
        .ifBlank { "." }
        .let { File(it).canonicalFile }
        .getOrTemp()
    Recipe.installNode(version, installationPath, OS.detect(), Arch.detect())
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")
fun input(name: String) = System.getenv("input_$name") ?: ""

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    writeError("$it (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    kotlin.system.exitProcess(1)
}

object Recipe {
    fun installNode(version: String, installationPath: File, os: OS, arch: Arch) {
        val nodeVersionManager = NodeJsVersionManager(version, os, arch)
        val downloadInfo = nodeVersionManager.getDownloadInfo()
        val unpackPath = File(installationPath, downloadInfo.filename)
        val binPath = getBinPath(unpackPath)
        if (binPath.isDirectory && binPath.listFiles()?.isNotEmpty() == true) {
            println("NodeJs ${downloadInfo.version} is already installed to ${unpackPath.absolutePath}")
        } else {
            println("Downloading NodeJs from ${downloadInfo.downloadUrl}")

            val archivePath = downloadToTemp(downloadInfo)
            println("Downloaded to ${archivePath.absolutePath}")

            Unpack.unpackFile(archivePath, installationPath)
            println("Unpacked to ${unpackPath.absolutePath}")
        }

        addToPath(binPath.absolutePath)
    }

    private fun downloadToTemp(info: NodeJsDownloadInfo): File =
        File(getTempDirectory(), info.filenameWithExtension).also { copyURLToFile(info.downloadUrl, it) }

    private fun getBinPath(unpackPath: File): File {
        val binPath = when {
            SystemUtils.IS_OS_WINDOWS -> unpackPath
            else -> unpackPath.resolve("bin")
        }
        return binPath
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
}

class NodeJsVersionManager(private val versionPrefix: String, private val os: OS, private val arch: Arch) {
    private val distUrl = "https://nodejs.org/dist/"

    fun getDownloadInfo(): NodeJsDownloadInfo = runBlocking {
        val json = downloadJson("${distUrl}index.json")
        val nodeVersions = parseJson(json)

        val nodeVersion = nodeVersions.find { it.version.startsWith("v$versionPrefix") }
        if (nodeVersion == null) {
            error("Could not find $versionPrefix")
        }

        val expectedName = when (os) {
            OS.Linux -> "linux-$arch"
            OS.Mac -> "osx-$arch-tar"
            OS.Windows -> "win-$arch-exe"
        }
        val file = nodeVersion.files.find { it.contains(expectedName) }
        if (file == null) {
            error("Could not detect node dist to download, available files: ${nodeVersion.files.joinToString(", ")}")
        }

        val version = nodeVersion.version
        val extension = if (os == OS.Windows) "zip" else "tar.gz"
        val urlBuilder = NodeJsDownloadInfo(
            distUrl,
            version,
            os.toString(),
            arch.toString(),
            extension
        )

        return@runBlocking urlBuilder
    }

    private suspend fun downloadJson(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
    }

    private fun parseJson(json: String): List<NodeJsVersion> {
        val mapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(json)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class NodeJsVersion(
    val version: String,
    val date: String,
    val files: List<String>
)

data class NodeJsDownloadInfo(
    val distUrl: String,
    val version: String,
    val os: String,
    val arch: String,
    val extension: String
) {
    val downloadUrl: URL
        get() = URL("$distUrl$version/$filenameWithExtension")

    val filename: String
        get() = "node-$version-$os-$arch"

    val filenameWithExtension: String
        get() = "$filename.$extension"
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
            else -> throw IllegalArgumentException("Unsupported OS")
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
            else -> throw IllegalArgumentException("Unsupported architecture: $arch")
        }

        private fun isProcTranslated() = runCatching {
            Runtime.getRuntime().exec(arrayOf("sysctl", "-n", "sysctl.proc_translated"))
                .inputStream.bufferedReader().use { it.readLine() == "1" }
        }.getOrDefault(false)
    }
}

object Unpack {
    fun unpackFile(filePath: File, outputDir: File) = when {
        filePath.name.endsWith(".tar.gz") -> unpackTarGz(filePath, outputDir)
        filePath.extension.equals("zip", ignoreCase = true) -> unpackZip(filePath, outputDir)
        else -> throw IllegalArgumentException("Unsupported file type: ${filePath.extension}")
    }

    private fun unpackZip(zipFile: File, outputDir: File) {
        outputDir.createIfNotExists()

        ZipFile.builder().setFile(zipFile).get().use { zip ->
            zip.entries.iterator().forEach { entry ->
                unpackArchiveEntry(entry, outputDir) { file ->
                    zip.getInputStream(entry).use { input ->
                        file.outputStream().use { out -> IOUtils.copy(input, out) }
                        file.setFilePermissions(entry.unixMode)
                    }
                }
            }
        }
    }

    private fun unpackTarGz(tarGzFile: File, outputDir: File) {
        outputDir.createIfNotExists()

        TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(tarGzFile))).use { tar ->
            generateSequence { tar.nextEntry }.forEach { entry ->
                unpackArchiveEntry(entry, outputDir) { file ->
                    file.outputStream().use { out -> tar.copyTo(out) }
                    file.setFilePermissions(entry.mode)
                }
            }
        }
    }

    private fun unpackArchiveEntry(entry: ArchiveEntry, dir: File, extract: (file: File) -> Unit) {
        dir.resolveUnpackLocation(entry.name).run {
            if (entry.isDirectory) {
                mkdirs()
            } else {
                parentFile?.mkdirs()
                extract(this)
            }
        }
    }

    private fun File.resolveUnpackLocation(entryName: String) = resolve(entryName).normalize().also {
        if (!it.canonicalPath.startsWith(canonicalPath))
            throw SecurityException("Archive entry is outside of the target directory: $entryName")
    }

    private fun File.createIfNotExists() = run { if (!exists()) mkdirs() }

    private fun File.setFilePermissions(mode: Int) {
        setReadable((mode and 0b100_000_000) != 0)
        setWritable((mode and 0b010_000_000) != 0)
        setExecutable((mode and 0b001_000_000) != 0)
    }
}

fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
fun writeWarning(text: String) = writeMessage(text, "status" to "WARNING")
fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
    println(asString(MESSAGE, mapOf("text" to text, *attributes)))

fun File.getOrTemp(): File = this.toPath().getOrTemp().toFile()

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