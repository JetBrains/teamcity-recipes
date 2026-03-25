@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

try {
    val workingDir = Paths.get(System.getProperty("user.dir"))
    val tempDir = Paths.get(System.getProperty("teamcity.build.tempDir") ?: System.getProperty("java.io.tmpdir"))

    val inputs = RecipeInputs()
    val installationType = when (val value = inputs.installationType.getRequired()) {
        "local" -> InstallationType.LOCAL
        "global" -> {
            val toolVersionInput = inputs.globalToolVersion
            toolVersionInput.getRequired("${toolVersionInput.label} must be specified for the installation type \"$value\"")
            if (inputs.globalToolPath.get() != null) InstallationType.GLOBAL_TOOL_PATH else InstallationType.GLOBAL
        }

        else -> {
            ServiceMessages.error("Invalid value of parameter \"${inputs.installationType.label}\": $value")
            throw StepExecutionException()
        }
    }

    val inspectionsFile: String = inputs.reportPath.get() ?: tempDir.resolve("inspections.xml").toString()

    runWithinBlock("Verifying .NET CLI is available") {
        val result = Processes.run(listOf("dotnet", "--version"), workingDir) // limited to CLI check as some users might want to have runtime only
        failStepIfError(result, "Failed to verify .NET CLI is available")
    }

    runWithinBlock("Installing inspections tool") {
        val result = Processes.run(installationType.composeInstallCommand(inputs), workingDir)
        failStepIfError(result, "Failed to install inspections tool")
    }

    runWithinBlock("Running inspections") {
        val inspectcodeArguments = mutableListOf(inputs.solutionFile.getRequired(), "-f=xml", "-o=$inspectionsFile")
        inputs.additionalRunArgs.get()?.let { inspectcodeArguments.addAll(splitJoinedArgs(it)) }
        val result = Processes.run(installationType.composeRunCommand(inputs, inspectcodeArguments), workingDir)
        failStepIfError(result, "Failed to run inspections")
    }

    runWithinBlock("Importing inspections report") {
        println("Importing inspections from $inspectionsFile")
        println(asString("importData", mapOf("type" to "ReSharperInspectCode", "path" to inspectionsFile)))
    }
} catch (_: StepExecutionException) {
    exitProcess(1)
} catch (e: Exception) {
    ServiceMessages.error("Exception caught during execution")
    ServiceMessages.error(e.stackTraceToString())
    exitProcess(1)
}

fun runWithinBlock(blockName: String, action: (blockName: String) -> Unit) {
    println(asString(BLOCK_OPENED, mapOf("name" to blockName)))
    try {
        action(blockName)
    } finally {
        println(asString(BLOCK_CLOSED, mapOf("name" to blockName)))
    }
}

fun failStepIfError(result: Processes.ExecutionResult, generalErrorMessage: String) {
    when (result) {
        is Processes.ExecutionResult.Success -> {}
        is Processes.ExecutionResult.Error -> {
            when (result) {
                is Processes.ExecutionResult.Error.NonZeroCode -> {
                    ServiceMessages.error("Process exited with code ${result.exitCode}")
                }

                is Processes.ExecutionResult.Error.Exception -> {
                    ServiceMessages.error("Exception caught during process execution")
                    ServiceMessages.error(result.reason.stackTraceToString())
                }

                is Processes.ExecutionResult.Error.Timeout -> {
                    ServiceMessages.error("Process shut down after timeout of ${result.duration}")
                }
            }

            ServiceMessages.error(generalErrorMessage)
            throw StepExecutionException()
        }
    }
}

fun splitJoinedArgs(input: String): List<String> {
    val args = mutableListOf<String>()
    val current = StringBuilder()
    var openQuote: Char? = null

    for (char in input) {
        when {
            openQuote != null -> {
                if (char == openQuote) openQuote = null
                else current.append(char)
            }

            char == '"' || char == '\'' -> openQuote = char
            char == ' ' -> {
                if (current.isNotEmpty()) {
                    args.add(current.toString())
                    current.clear()
                }
            }

            else -> current.append(char)
        }
    }

    if (current.isNotEmpty()) args.add(current.toString())
    return args
}

object Processes {

    fun run(command: List<String>, workingDir: Path, additionalEnvVars: Map<String, String> = emptyMap()): ExecutionResult = runBlocking {
        try {
            val workingDirNormalized = workingDir.toAbsolutePath().normalize()
            println("Starting: ${command.joinToString(" ")}")
            println("in directory: $workingDirNormalized")
            println("(see verbose logs for environment variables)")

            val process = ProcessBuilder(command)
                .directory(workingDirNormalized.toFile())
                .redirectErrorStream(false)
                .apply {
                    environment().putAll(additionalEnvVars)
                    ServiceMessages.debug("with environment variables:")
                    environment().forEach { ServiceMessages.debug("${it.key}=${it.value}") }
                }
                .start()

            val systemOutJob = readLinesAsync(process.inputStream) { println(it) }
            val systemErrJob = readLinesAsync(process.errorStream) { System.err.println(it) }

            val timeoutDurationHours = 10L
            if (!process.waitFor(timeoutDurationHours, TimeUnit.HOURS)) {
                systemOutJob.cancel()
                systemErrJob.cancel()
                process.destroy()
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    process.waitFor()
                }

                return@runBlocking ExecutionResult.Error.Timeout(timeoutDurationHours.toDuration(DurationUnit.HOURS))
            }

            systemOutJob.join()
            systemErrJob.join()
            val exitCode = process.exitValue()
            return@runBlocking if (exitCode == 0) ExecutionResult.Success else ExecutionResult.Error.NonZeroCode(exitCode)
        } catch (e: Exception) {
            return@runBlocking ExecutionResult.Error.Exception(e)
        }
    }

    private fun CoroutineScope.readLinesAsync(inputStream: InputStream, lineConsumer: (String) -> Unit) =
        launch(Dispatchers.IO) {
            inputStream.bufferedReader().forEachLine { lineConsumer.invoke(it) }
        }

    sealed interface ExecutionResult {
        data object Success : ExecutionResult

        sealed interface Error : ExecutionResult {
            data class NonZeroCode(val exitCode: Int) : Error
            data class Exception(val reason: Throwable) : Error
            data class Timeout(val duration: Duration) : Error
        }
    }
}

enum class InstallationType {
    LOCAL {
        override fun composeInstallCommand(inputs: RecipeInputs): List<String> {
            println("Restoring local tools")
            return listOf("dotnet", "tool", "restore")
        }

        override fun composeRunCommand(inputs: RecipeInputs, inspectcodeArguments: List<String>): List<String> {
            println("Running ${ReSharperConstants.TOOL_NAME} as local tool")
            return listOf("dotnet", "tool", "run", ReSharperConstants.JB_EXECUTABLE, ReSharperConstants.INSPECTCODE_TOOL_NAME) + inspectcodeArguments
        }
    },
    GLOBAL {
        override fun composeInstallCommand(inputs: RecipeInputs): List<String> {
            val version = inputs.globalToolVersion.getRequired()
            println("Installing ${ReSharperConstants.TOOL_NAME} $version as global tool")
            return listOf("dotnet", "tool", "install", ReSharperConstants.TOOL_NAME, "--global", "--version", version, "--allow-downgrade")
        }

        override fun composeRunCommand(inputs: RecipeInputs, inspectcodeArguments: List<String>): List<String> {
            println("Running ${ReSharperConstants.TOOL_NAME} as global tool")
            return listOf(ReSharperConstants.JB_EXECUTABLE, ReSharperConstants.INSPECTCODE_TOOL_NAME) + inspectcodeArguments
        }
    },
    GLOBAL_TOOL_PATH {
        override fun composeInstallCommand(inputs: RecipeInputs): List<String> {
            val version = inputs.globalToolVersion.getRequired()
            val toolPath = inputs.globalToolPath.getRequired()
            println("Installing ${ReSharperConstants.TOOL_NAME} $version as global tool at $toolPath")
            return listOf("dotnet", "tool", "install", ReSharperConstants.TOOL_NAME, "--tool-path", toolPath, "--version", version, "--allow-downgrade")
        }

        override fun composeRunCommand(inputs: RecipeInputs, inspectcodeArguments: List<String>): List<String> {
            val toolPath = inputs.globalToolPath.getRequired()
            val executable = Paths.get(toolPath).resolve(ReSharperConstants.JB_EXECUTABLE)
            println("Running ${ReSharperConstants.TOOL_NAME} as global tool at $toolPath")
            return listOf(executable.toString(), ReSharperConstants.INSPECTCODE_TOOL_NAME) + inspectcodeArguments
        }
    };

    abstract fun composeInstallCommand(inputs: RecipeInputs): List<String>

    abstract fun composeRunCommand(inputs: RecipeInputs, inspectcodeArguments: List<String>): List<String>
}

data class RecipeInputs(
    val solutionFile: Input = Input("solution_path", "Solution file path"),
    val installationType: Input = Input("installation_type", "Tool installation type"),
    val globalToolVersion: Input = Input("global_tool_version", "Tool version (global installation only)"),
    val globalToolPath: Input = Input("global_tool_path", "Tool path (global installation only)"),
    val reportPath: Input = Input("report_path", "Report path"),
    val additionalRunArgs: Input = Input("additional_run_args", "Additional InspectCode args"),
)

class Input(val name: String, val label: String) {

    fun get(): String? = System.getenv("input_$name")?.takeIf { it.isNotBlank() }

    fun getRequired(missingMessage: String = "Missing non-empty value for required parameter \"$label\""): String {
        val value = get()
        if (value == null) {
            ServiceMessages.error(missingMessage)
            throw StepExecutionException()
        }

        return value
    }
}

class StepExecutionException() : RuntimeException()

object ServiceMessages {
    fun debug(text: String) = message(text, TAGS_ATRRIBUTE to "tc:internal")
    fun warning(text: String) = message(text, "status" to "WARNING")
    fun error(text: String) = message(text, "status" to "ERROR")
    fun message(text: String, vararg attributes: Pair<String, String>) = println(asString(MESSAGE, mapOf("text" to text, *attributes)))
}

object ReSharperConstants {
    const val TOOL_NAME = "JetBrains.ReSharper.GlobalTools"
    const val JB_EXECUTABLE = "jb"
    const val INSPECTCODE_TOOL_NAME = "inspectcode"
}