package aws

import io.kotest.assertions.withClue
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class DownloadFromS3Test : AwsS3Test() {

    override val scriptPath: String = "../src/aws/download-from-s3/src.main.kts"

    init {
        "should download file from bucket root to existing target directory" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript().withS3Source("$bucket/file").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "text"
            }
        }

        "should download file to non-existing target directory" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript().withS3Source("$bucket/file")
                .withTarget(target.absolutePathString() + "/new/").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "new/file"
                resolve("new/file").readText() shouldBe "text"
            }
        }

        "should download file to non-existing target directory with new file name" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript().withS3Source("$bucket/file")
                .withTarget(target.absolutePathString() + "/new/renamed").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "new/renamed"
                resolve("new/renamed").readText() shouldBe "text"
            }
        }

        "should download file when existing target directory is not empty" {
            // arrange
            val target = tempDir("target") {
                file("existing", "existing text")
            }
            tempFile(content = "new text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript().withS3Source("$bucket/file").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("existing", "file")
                resolve("existing").readText() shouldBe "existing text"
                resolve("file").readText() shouldBe "new text"
            }
        }

        "should download file from nested directory" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "dir/sub/file") }
            // act
            val result = readScript().withS3Source("$bucket/dir/sub/file").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "text"
            }
        }

        "should override existing file" {
            // arrange
            val target = tempDir("target") {
                file("file", "old text")
            }
            tempFile(content = "new text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript().withS3Source("$bucket/file").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "new text"
            }
        }

        "should download file with new name" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "old") }
            // act
            val result = readScript().withS3Source("$bucket/old").withTarget(target.resolve("renamed")).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "renamed"
                resolve("renamed").readText() shouldBe "text"
            }
        }

        "should download file with new name and override existing" {
            // arrange
            val target = tempDir("target") {
                file("renamed", "existing text")
            }
            tempFile(content = "new text").also { bucket.putObject(it, key = "old") }
            // act
            val result = readScript().withS3Source("$bucket/old").withTarget(target.resolve("renamed")).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "renamed"
                resolve("renamed").readText() shouldBe "new text"
            }
        }

        "should download file when ./file relative path is used" {
            // arrange
            tempFile(name = "file", content = "text").also { bucket.putObject(it, key = "file") }
            val target = tempDir("target")
                .also { Files.list(it).collect(Collectors.toList()) shouldHaveSize 0 }
            // act
            val result = readScript()
                .withWorkingDir(target)
                .withS3Source("$bucket/file")
                .withTarget("./file")
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "text"
            }
        }

        "should download file when . relative path is used" {
            // arrange
            tempFile(name = "file", content = "text").also { bucket.putObject(it, key = "file") }
            val target = tempDir("target")
                .also { Files.list(it).collect(Collectors.toList()) shouldHaveSize 0 }
            // act
            val result = readScript()
                .withWorkingDir(target)
                .withS3Source("$bucket/file")
                .withTarget(".")
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "text"
            }
        }

        "should download directory content" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text 1").also { bucket.putObject(it, key = "dir/file1") }
            tempFile(content = "text 2").also { bucket.putObject(it, key = "dir/file2") }
            tempFile(content = "text 3").also { bucket.putObject(it, key = "dir/sub/file3") }
            // act
            val result = readScript().withS3Source("$bucket/dir").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("file1", "file2", "sub/file3")
                resolve("file1").readText() shouldBe "text 1"
                resolve("file2").readText() shouldBe "text 2"
                resolve("sub/file3").readText() shouldBe "text 3"
            }
        }

        "should download directory content to non-existing target directory" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text 1").also { bucket.putObject(it, key = "dir/file1") }
            tempFile(content = "text 2").also { bucket.putObject(it, key = "dir/sub/file2") }
            // act
            val result = readScript().withS3Source("$bucket/dir").withTarget(target.resolve("new")).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("new/file1", "new/sub/file2")
                resolve("new/file1").readText() shouldBe "text 1"
                resolve("new/sub/file2").readText() shouldBe "text 2"
            }
        }

        "should download directory content from subdirectory" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text 1").also { bucket.putObject(it, key = "dir/sub/file1") }
            tempFile(content = "text 2").also { bucket.putObject(it, key = "dir/sub/sub2/file2") }
            // act
            val result = readScript().withS3Source("$bucket/dir/sub").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("file1", "sub2/file2")
                resolve("file1").readText() shouldBe "text 1"
                resolve("sub2/file2").readText() shouldBe "text 2"
            }
        }

        "should download directory content to non-empty target directory" {
            // arrange
            val target = tempDir("target") {
                file("file1", "should be overridden")
                file("file2", "old content")
            }
            tempFile(content = "overridden content").also { bucket.putObject(it, key = "dir/file1") }
            tempFile(content = "new content").also { bucket.putObject(it, key = "dir/sub/file3") }
            // act
            val result = readScript().withS3Source("$bucket/dir").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("file1", "file2", "sub/file3")
                resolve("file1").readText() shouldBe "overridden content"
                resolve("file2").readText() shouldBe "old content"
                resolve("sub/file3").readText() shouldBe "new content"
            }
        }

        "should download empty subdirectory" {
            // arrange
            val target = tempDir("target")
            bucket.createEmptyDir("dir/")
            bucket.createEmptyDir("dir/sub")
            // act
            val result = readScript().withS3Source("$bucket/dir").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("sub")
                resolve("sub").isDirectory() shouldBe true
            }
        }

        "should download empty subdirectory when source with trailing slash" {
            // arrange
            val target = tempDir("target")
            bucket.createEmptyDir("dir/")
            bucket.createEmptyDir("dir/sub/")
            // act
            val result = readScript().withS3Source("$bucket/dir/").withTarget(target).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly listOf("sub")
                resolve("sub").isDirectory() shouldBe true
            }
        }

        "should download with credentials and AWS region from inputs" {
            // arrange
            val target = tempDir("target")
            tempFile(content = "text").also { bucket.putObject(it, key = "file") }
            // act
            val result = readScript()
                .withNewEnv(mapOf("MIN_IO_ENDPOINT" to endpoint))
                .withS3Source("$bucket/file")
                .withTarget(target)
                .withInput("aws_access_key_id", user)
                .withInput("aws_secret_access_key", password)
                .withInput("aws_region", region.id())
                .also { it.env shouldNotContainKey "AWS_ACCESS_KEY_ID" }
                .also { it.env shouldNotContainKey "AWS_SECRET_ACCESS_KEY" }
                .also { it.env shouldNotContainKey "AWS_REGION" }
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(target) {
                this shouldContainOnly "file"
                resolve("file").readText() shouldBe "text"
            }
        }

        "should fail to download when source does not exist in S3" {
            // arrange
            val target = tempDir("target")
            // act
            val result = readScript().withS3Source("$bucket/fake").withTarget(target).eval()
            // assert
            result.exitCode shouldBe 1
            result.stdout shouldContain "S3 object not found at s3://test-bucket/fake"
            target.shouldBeEmpty()
        }

        "should fail to download when bucket does not exist" {
            // act
            val result = readScript().withS3Source("fake-bucket").withTarget(tempDir("target")).eval()
            // assert
            result.exitCode shouldBe 1
            result.stdout shouldContain "verify the bucket name"
        }

        "should fail to download when source is not valid" {
            // act
            val result = readScript().withSource("wrong source").withTarget(tempDir("target")).eval()
            // assert
            result.exitCode shouldBe 1
            result.stdout shouldContain "Source should be a path to S3 object"
        }
    }

    private val bucket = Bucket("test-bucket")

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        s3Client.createBucket { it.bucket(bucket.name) }
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        bucket.deleteAllObjects()
    }

    private fun Script.withSource(source: String) = withInput("source", source)
    private fun Script.withS3Source(source: String) = withSource("s3://$source")
    private fun Script.withTarget(target: String) = withInput("target", target)
    private fun Script.withTarget(target: Path) = withInput("target", target.absolutePathString())

    private fun Path.shouldBeEmpty() = shouldContainOnly(listOf())

    private infix fun Path.shouldContainOnly(file: String) = shouldContainOnly(listOf(file))

    private infix fun Path.shouldContainOnly(files: List<String>) {
        withClue("""Expected "${this.name}" to be a directory""") { this.isDirectory() shouldBe true }
        val actualPaths = Files.walk(this)
            .filter { it != this }
            .filter { Files.isRegularFile(it) || (it.isDirectory() && !Files.list(it).findAny().isPresent) }
            .map { this.relativize(it).toString().replace("\\", "/") }
            .sorted()
            .collect(Collectors.toList())
        if (files.isEmpty()) {
            actualPaths shouldHaveSize 0
        } else {
            actualPaths.shouldContainExactly(files.sorted())
        }
    }
}
