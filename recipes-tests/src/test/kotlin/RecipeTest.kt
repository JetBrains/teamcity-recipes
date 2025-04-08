import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.*

abstract class RecipeTest : StringSpec() {

    protected abstract val scriptPath: String

    private val tempFiles = ArrayList<Path>()

    @OptIn(ExperimentalPathApi::class)
    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        tempFiles.forEach { tempFile ->
            if (tempFile.isRegularFile()) {
                tempFile.deleteExisting()
            } else {
                tempFile.deleteRecursively()
            }
        }
    }

    protected open fun readScript(): Script {
        return Script(Paths.get(scriptPath).readText(Charsets.UTF_8))
    }

    protected fun tempFile(name: String = "teamcity-recipes-tmp-file-" + randomString(), content: String = ""): Path {
        return tempDir("teamcity-recipes-tmp-dir-" + randomString()) {
            file(name, content)
        }.resolve(name)
    }

    private fun randomString() = UUID.randomUUID().toString()

    protected fun tempDir(name: String): Path = tempDir(name) {}

    protected fun tempDir(name: String, block: TempDirBuilder.() -> Unit): Path {
        return Files
            .createTempDirectory("tmp")
            .also { tempFiles.add(it) }
            .resolve(name)
            .also { it.createDirectories() }
            .also { TempDirBuilder(it).apply(block) }
    }

    protected class TempDirBuilder(private val root: Path) {
        fun file(name: String, content: String) {
            root.resolve(name).also { it.writeText(content) }
        }

        fun dir(name: String, block: (TempDirBuilder.() -> Unit)? = null) {
            val subDir = root.resolve(name).also { it.createDirectories() }
            block?.let { TempDirBuilder(subDir).apply(it) }
        }
    }

    protected fun ScriptResult.shouldHaveZeroExitCode() {
        withClue("Exit code should be 0, $this") { exitCode shouldBe 0 }
    }

    protected inner class Script(private var content: String) {
        var workingDir: Path? = null
        val env = HashMap<String, String>()

        fun withInput(name: String, value: String) =
            withEnv("input_$name", value)

        fun withEnv(name: String, value: String): Script {
            env[name] = value
            return this
        }

        fun withNewEnv(env: Map<String, String>): Script {
            this.env.clear()
            this.env.putAll(env)
            return this
        }

        fun withWorkingDir(dir: Path): Script {
            this.workingDir = dir
            return this
        }

        fun withParameter(name: String, value: String): Script {
            content = content.replace("%$name%", value)
            return this
        }

        fun withCheckoutDir(path: Path): Script {
            return withParameter("teamcity.build.checkoutDir", path.toString())
        }

        fun eval(): ScriptResult {
            val tmpScriptFile = tempFile("recipe.main.kts", content)
            val exec = if (System.getProperty("os.name").lowercase().contains("windows")) "kotlin.bat" else "kotlin"
            val kotlin = Paths.get("build/kotlin-compiler/kotlinc/bin/$exec")
            val process = ProcessBuilder(kotlin.absolutePathString(), tmpScriptFile.absolutePathString())
                .apply { if (workingDir != null) directory(workingDir!!.toFile()) }
                .redirectErrorStream(false)
                .apply { environment().putAll(env) }
                .start()
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            return ScriptResult(process.waitFor(), stdout, stderr)
        }
    }

    protected data class ScriptResult(val exitCode: Int, val stdout: String, val stderr: String)
}
