import io.kotest.matchers.shouldBe
import kotlin.io.path.absolutePathString
import kotlin.io.path.isSymbolicLink

class SetupNodeTest : RecipeTest() {

    override val scriptPath: String = "../src/setup-node/src.main.kts"

    init {
        "should install node and preserve symlinks".config(
            enabled = !System.getProperty("os.name").lowercase().contains("windows")
        ) {
            // arrange
            val dir = tempDir(name = "node")
            val version = "22.13.1"

            // act
            val result = readScript()
                .withInput("version", version)
                .withInput("installation_path", dir.absolutePathString())
                .eval()

            // assert
            result.shouldHaveZeroExitCode()

            // Node.js tar.gz distributions contain symlinks in bin/
            // e.g. bin/npm -> ../lib/node_modules/npm/bin/npm-cli.js
            val nodeDir = dir.toFile().listFiles()
                ?.firstOrNull { it.name.startsWith("node-v$version") }
                ?: error("Node directory not found in ${dir}")
            val binDir = nodeDir.toPath().resolve("bin")

            val npm = binDir.resolve("npm")
            npm.isSymbolicLink() shouldBe true

            val npx = binDir.resolve("npx")
            npx.isSymbolicLink() shouldBe true
        }
    }
}
