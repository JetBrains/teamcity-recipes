import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList

abstract class RecipeTest : StringSpec() {

    private val tempFiles = CopyOnWriteArrayList<File>()

    @JvmInline
    protected value class Script(val content: String)

    protected data class ScriptResult(val exitCode: Int, val stdout: String, val stderr: String)

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        tempFiles.forEach { tempFile ->
            if (tempFile.isFile) tempFile.delete() else tempFile.deleteRecursively()
        }
    }

    protected fun readScript(path: String): Script {
        return Script(File(path).readText(Charsets.UTF_8))
    }

    protected fun Script.eval(envVariables: Map<String, String>): ScriptResult {
        val tmpScriptFile = createTempFile("tmp", "script.main.kts").also { it.writeText(content) }
        val exec = if (System.getProperty("os.name").lowercase().contains("windows")) "kotlin.bat" else "kotlin"
        val processBuilder = ProcessBuilder("build/kotlin-compiler/kotlinc/bin/$exec", tmpScriptFile.absolutePath)
                .redirectErrorStream(false)
        processBuilder.environment().putAll(envVariables)
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText().trim()
        return ScriptResult(process.waitFor(), stdout, stderr)
    }

    protected fun createTempFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix).also { tempFiles.add(it) }
    }
}