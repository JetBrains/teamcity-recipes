import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beBlank
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.fail
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class DaggerTest : RecipeTest() {

    override val scriptPath: String = "../src/dagger/src.main.kts"

    init {

        "happy path: install dagger, call command, stop engine" {
            // arrange
            val dir = tempDir(name = "dagger")
            val daggerModule = "test-module"
            // act
            val result = readScript()
                .withInput("version", "0.18.2")
                .withInput("installation_path", dir.absolutePathString())
                .withInput("workdir", dir.absolutePathString())
                .withInput("command", "dagger init --name $daggerModule")
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            checkDaggerInstalled(dir, version = "0.18.2")
            result.stdout shouldContain "Initialized module $daggerModule"
            withClue("dagger engine should be stopped") {
                runProcess(listOf("docker", "ps", "--filter", "name=dagger-engine-*", "-q"), dir).let {
                    it.stdout should beBlank()
                }
            }
        }

        "should not stop engine after running command when explicitly configured" {
            // arrange
            val dir = tempDir(name = "dagger")
            // act
            readScript()
                .withInput("stop_engine", "false")
                .withInput("version", "0.18.2")
                .withInput("installation_path", dir.absolutePathString())
                .withInput("workdir", dir.absolutePathString())
                .withInput("command", "dagger init --name test-module") // command is needed to start engine
                .eval()
            // assert
            withClue("dagger engine should keep running") {
                runProcess(listOf("docker", "ps", "--filter", "name=dagger-engine-*", "-q"), dir).let {
                    val stdout = it.stdout.split(System.lineSeparator()).filter { line -> line.isNotBlank() }
                    stdout shouldHaveSize 1
                    runProcess(listOf("docker", "stop", "-t", "300", stdout.first()), dir)
                }
            }
        }

        "should install the latest available dagger version" {
            // arrange
            val latestVersion = """"tag_name"\s*:\s*"(v[0-9.]+)""""
                .toRegex()
                .find(URI("https://api.github.com/repos/dagger/dagger/releases?per_page=1").toURL().readText())
                ?.groupValues
                ?.get(1)
                ?.removePrefix("v")
                ?: fail("Failed to fetch the latest Dagger version")
            println("Installing Dagger $latestVersion")
            val dir = tempDir(name = "dagger")
            // act
            val result = readScript()
                .withInput("version", latestVersion)
                .withInput("installation_path", dir.absolutePathString())
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            checkDaggerInstalled(dir, latestVersion)
        }

        "should install old (0.9.0) dagger version" {
            // arrange
            val dir = tempDir(name = "dagger")
            // act
            val result = readScript()
                .withInput("version", "0.9.0")
                .withInput("installation_path", dir.absolutePathString())
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            checkDaggerInstalled(dir, "0.9.0")
        }

        "should install dagger when version starts with 'v' prefix" {
            // arrange
            val dir = tempDir(name = "dagger")
            // act
            val result = readScript()
                .withInput("version", "v0.18.2")
                .withInput("installation_path", dir.absolutePathString())
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            checkDaggerInstalled(dir, version = "0.18.2")
        }

        "should install multiple dagger versions" {
            // arrange
            val dir = tempDir(name = "dagger")
            val versions = listOf("0.18.2", "0.18.1")
            // act
            versions.forEach { version ->
                readScript()
                    .withInput("version", version)
                    .withInput("installation_path", dir.absolutePathString())
                    .eval()
                    .also { it.shouldHaveZeroExitCode() }
            }
            // assert
            versions.forEach { version ->
                checkDaggerInstalled(dir, version)
                checkDaggerInstalled(dir, version)
            }
        }
    }

    private fun checkDaggerInstalled(rootDir: Path, version: String) {
        val daggerDirName = "dagger-$version"
        val daggerDir = rootDir.resolve(daggerDirName)
        val dagger = daggerDir.resolve("dagger")
        withClue("Dagger should be installed to $daggerDirName") {
            daggerDir.isDirectory() shouldBe true
            dagger.isRegularFile() shouldBe true
        }
        val result = runProcess(listOf(dagger.absolutePathString(), "version"), rootDir)
        result.exitCode shouldBe 0
        result.stdout shouldContain version
    }

    private data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun runProcess(cmd: List<String>, dir: Path): ProcessResult {
        val process = ProcessBuilder(cmd).directory(dir.toFile()).start()
        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText().trim()
        return ProcessResult(process.waitFor(), stdout, stderr)
    }
}
