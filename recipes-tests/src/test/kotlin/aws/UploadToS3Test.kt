package aws

import aws.UploadToS3Test.ExpectedResult.CorrectS3Dir
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import software.amazon.awssdk.services.s3.model.S3Object
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

class UploadToS3Test : AwsS3Test() {

    override val scriptPath: String = "../src/aws/upload-to-s3/src.main.kts"

    init {
        "should upload file to bucket root" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result = readScript().withCheckoutDir(file.parent).withSource(file).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey file.name
                this withKey file.name inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to bucket root with trailing slash" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result = readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey file.name
                this withKey file.name inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to bucket root with new file name" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result =
                readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/renamed").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "renamed"
                this withKey "renamed" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to non-existing directory" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result = readScript()
                .withCheckoutDir(file.parent)
                .withSource(file)
                .withS3Target("$bucket/dir/") // trailing slash is needed to indicate a new dir, not a new file name
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "dir/${file.name}"
                this withKey "dir/${file.name}" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to non-existing nested directory" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result = readScript()
                .withCheckoutDir(file.parent)
                .withSource(file)
                .withS3Target("$bucket/dir/sub/")
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "dir/sub/${file.name}"
                this withKey "dir/sub/${file.name}" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to existing directory" {
            // arrange
            val file = tempFile(content = "text")
            bucket.createEmptyDir("dir/")
            // act
            val result = readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/dir/").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/", "dir/${file.name}")
                this withKey "dir/" shouldBe CorrectS3Dir
                this withKey "dir/${file.name}" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to existing directory without trailing slash in target path" {
            // arrange
            val file = tempFile(content = "text")
            bucket.createEmptyDir("dir/")
            // act
            val result = readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/dir").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/", "dir/${file.name}")
                this withKey "dir/" shouldBe CorrectS3Dir
                this withKey "dir/${file.name}" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to non-existing directory with new file name" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result =
                readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/dir/renamed").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "dir/renamed"
                this withKey "dir/renamed" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload file to existing directory with new file name" {
            // arrange
            val file = tempFile(content = "text")
            bucket.createEmptyDir("dir/")
            // act
            val result =
                readScript().withCheckoutDir(file.parent).withSource(file).withS3Target("$bucket/dir/renamed").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/", "dir/renamed")
                this withKey "dir/" shouldBe CorrectS3Dir
                this withKey "dir/renamed" inBucket bucket shouldHaveContent "text"
            }
        }

        "should override file when already exists" {
            // arrange
            val file = tempFile(content = "old text")
                .also { bucket.putObject(it) }
                .also { it.writeText("new text") }
            // act
            val result = readScript().withCheckoutDir(file.parent).withSource(file).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey file.name
                this withKey file.name inBucket bucket shouldHaveContent "new text"
            }
        }

        "should upload file when ./file relative path is used" {
            // arrange
            val file = tempDir("tmp") { file("file", "text") }.resolve("file")
            // act
            val result = readScript()
                .withCheckoutDir(file.parent)
                .withWorkingDir(file.parent)
                .withSource("./file")
                .withS3Target(bucket)
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "file"
                this withKey "file" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload directory content to bucket root" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text 1")
                dir("c") {
                    file("d", "text 2")
                }
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("b", "c/d")
                this withKey "b" inBucket bucket shouldHaveContent "text 1"
                this withKey "c/d" inBucket bucket shouldHaveContent "text 2"
            }
        }

        "should upload directory content to a non-existing target directory" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text 1")
                dir("c") {
                    file("d", "text 2")
                }
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/b", "dir/c/d")
                this withKey "dir/b" inBucket bucket shouldHaveContent "text 1"
                this withKey "dir/c/d" inBucket bucket shouldHaveContent "text 2"
            }
        }

        "should upload directory content to a non-existing target sub directory" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text 1")
                dir("c") {
                    file("d", "text 2")
                }
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir/sub").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/sub/b", "dir/sub/c/d")
                this withKey "dir/sub/b" inBucket bucket shouldHaveContent "text 1"
                this withKey "dir/sub/c/d" inBucket bucket shouldHaveContent "text 2"
            }
        }

        "should upload directory content to a non-existing target directory with trailing slash" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text")
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir/").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "dir/b"
                this withKey "dir/b" inBucket bucket shouldHaveContent "text"
            }
        }

        "should upload directory content to an existing empty target directory" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text 1")
                dir("c") {
                    file("d", "text 2")
                }
            }
            bucket.createEmptyDir("dir/")
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/", "dir/b", "dir/c/d")
                this withKey "dir/b" inBucket bucket shouldHaveContent "text 1"
                this withKey "dir/c/d" inBucket bucket shouldHaveContent "text 2"
                this withKey "dir/" shouldBe CorrectS3Dir
            }
        }

        "should upload directory content to an existing empty target directory with trailing slash" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text 1")
                dir("c") {
                    file("d", "text 2")
                }
            }
            bucket.createEmptyDir("dir/")
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir/").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/", "dir/b", "dir/c/d")
                this withKey "dir/b" inBucket bucket shouldHaveContent "text 1"
                this withKey "dir/c/d" inBucket bucket shouldHaveContent "text 2"
            }
        }

        "should upload directory content to an existing non-empty target directory preserving existing files" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text from uploaded file")
            }
            bucket.putObject(tempFile(content = "text from existing file"), "dir/existing-1")
            bucket.putObject(tempFile(content = "text from existing file 2"), "dir/sub/existing-2")
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/b", "dir/existing-1", "dir/sub/existing-2")
                this withKey "dir/b" inBucket bucket shouldHaveContent "text from uploaded file"
                this withKey "dir/existing-1" inBucket bucket shouldHaveContent "text from existing file"
                this withKey "dir/sub/existing-2" inBucket bucket shouldHaveContent "text from existing file 2"
            }
        }

        "should not upload empty directory" {
            // arrange
            val dir = tempDir("a")
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            result.stdout shouldContain "nothing to upload"
            bucket.listAllObjects() shouldBe emptyList()
        }

        "should upload a single empty subdirectory" {
            // arrange
            val dir = tempDir("a") {
                dir("b")
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey "b/"
                this withKey "b/" shouldBe CorrectS3Dir
            }
        }

        "should upload with empty subdirectories" {
            // arrange
            val dir = tempDir("a") {
                file("b", "text")
                dir("c")
                file("d", "text")
                dir("e") {
                    dir("f")
                }
            }
            // act
            val result = readScript().withCheckoutDir(dir).withSource(dir).withS3Target("$bucket/dir").eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKeys listOf("dir/b", "dir/c/", "dir/d", "dir/e/f/")
                this withKey "dir/c/" shouldBe CorrectS3Dir
                this withKey "dir/e/f/" shouldBe CorrectS3Dir
            }
        }

        "should upload directory content when star wildcard is used" {
            // arrange
            val dir = tempDir("a") {
                file("b.txt", "")
                file("c.doc", "")
                file("d.txt", "")
                dir("e") {
                    file("f.txt", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("*.txt").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("b.txt", "d.txt")
        }

        "should upload directory content when only star wildcard is used" {
            // arrange
            val dir = tempDir("a") {
                file("b.txt", "")
                file("c.txt", "")
                dir("d") {
                    file("e.txt", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("*").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("b.txt", "c.txt", "d/e.txt")
        }

        "should upload directory content when double star wildcard is used" {
            // arrange
            val dir = tempDir("a") {
                file("b.txt", "")
                file("c.doc", "")
                file("d.txt", "")
                dir("e") {
                    file("f.txt", "")
                    file("g.doc", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("**.txt").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("b.txt", "d.txt", "e/f.txt")
        }

        "should upload directory content when only double star wildcard is used" {
            // arrange
            val dir = tempDir("a") {
                file("b.txt", "")
                file("c.doc", "")
                dir("d") {
                    file("f.txt", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("**").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("b.txt", "c.doc", "d/f.txt")
        }

        "should upload directory content when question mark wildcard is used" {
            // arrange
            val dir = tempDir("a") {
                file("f1.txt", "")
                file("f2.txt", "")
                file("f10.txt", "")
                dir("d") {
                    file("f3.txt", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("f?.txt").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("f1.txt", "f2.txt")
        }

        "should upload directory content when wildcards are combined" {
            // arrange
            val dir = tempDir("a") {
                file("f10", "")
                file("f-abc-11", "")
                file("f20", "")
                dir("d") {
                    file("f12", "")
                }
            }
            // act
            val result = readScript().withWorkingDir(dir).withCheckoutDir(dir)
                .withSource("**f*1?").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("f10", "f-abc-11", "d/f12")
        }

        "should upload directory content when wildcard is after an absolute path" {
            // arrange
            val dir = tempDir("a") {
                file("b.txt", "")
                file("c.doc", "")
                file("d.txt", "")
                dir("e") {
                    file("f.txt", "")
                }
            }
            // act
            val result = readScript().withCheckoutDir(dir)
                .withSource(dir.absolutePathString() + "/*.txt").withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys listOf("b.txt", "d.txt")
        }

        val filesWithWildcardsProvider = {
            tempDir("files_with_wildcards") {
                file("file-with-*-in-name.txt", "")
                file("file-with-**-in-name.txt", "")
                file("file-with-?-in-name.txt", "")
                file("*", "")
                file("**", "")
                file("?", "")
            }
        }

        "should upload files when they have names with symbols that we consider as wildcards" {
            // arrange
            val dir = filesWithWildcardsProvider()
            // act, assert
            dir.listDirectoryEntries().forEach { file ->
                val result =
                    readScript().withWorkingDir(dir).withCheckoutDir(dir).withSource(file).withS3Target(bucket).eval()
                result.shouldHaveZeroExitCode()
            }
            bucket.listAllObjects() shouldContainOnlyKeys dir.listDirectoryEntries().map { it.name }
        }

        "should upload directory with files when they have names with symbols that we consider as wildcards" {
            // arrange
            val dir = filesWithWildcardsProvider()
            // act
            val result =
                readScript().withWorkingDir(dir).withCheckoutDir(dir).withSource(dir).withS3Target(bucket).eval()
            // assert
            result.shouldHaveZeroExitCode()
            bucket.listAllObjects() shouldContainOnlyKeys dir.listDirectoryEntries().map { it.name }
        }

        "should upload with credentials and AWS region from inputs" {
            // arrange
            val file = tempFile(content = "text")
            // act
            val result = readScript()
                .withCheckoutDir(file.parent)
                .withNewEnv(mapOf("MIN_IO_ENDPOINT" to endpoint))
                .withSource(file)
                .withS3Target(bucket)
                .withInput("aws_access_key_id", user)
                .withInput("aws_secret_access_key", password)
                .withInput("aws_region", region.id())
                .also { it.env shouldNotContainKey "AWS_ACCESS_KEY_ID" }
                .also { it.env shouldNotContainKey "AWS_SECRET_ACCESS_KEY" }
                .also { it.env shouldNotContainKey "AWS_REGION" }
                .eval()
            // assert
            result.shouldHaveZeroExitCode()
            with(bucket.listAllObjects()) {
                this shouldContainOnlyKey file.name
                this withKey file.name inBucket bucket shouldHaveContent "text"
            }
        }

        "should fail to upload when source does not exist locally" {
            // act
            val result = readScript()
                .withCheckoutDir(Paths.get("/fake")).withSource("/fake/file.txt").withS3Target(bucket)
                .eval()
            // assert
            result.exitCode shouldBe 1
            result.stdout shouldContain "File or directory does not exist at /fake/file.txt"
            bucket.listAllObjects().size shouldBe 0
        }

        "should fail to upload when bucket does not exist" {
            // arrange
            val source = tempFile()
            // act
            val result =
                readScript().withCheckoutDir(source.parent).withSource(source).withS3Target("fake-bucket").eval()
            // assert
            result.exitCode shouldBe 1
            result.stdout shouldContain "verify the bucket name"
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
    private fun Script.withSource(source: Path) = withSource(source.absolutePathString())
    private fun Script.withTarget(target: String) = withInput("target", target)
    private fun Script.withS3Target(target: String) = withTarget("s3://$target")
    private fun Script.withS3Target(target: Bucket) = withS3Target(target.name)

    private infix fun List<S3Object>.withKey(key: String): S3Object = first { it.key() == key }
    private infix fun S3Object.inBucket(bucket: Bucket): S3ObjectInBucket = S3ObjectInBucket(this, bucket)
    private infix fun List<S3Object>.shouldContainOnlyKeys(keys: List<String>) =
        map { it.key() } shouldContainOnly keys

    private infix fun List<S3Object>.shouldContainOnlyKey(key: String) = shouldContainOnlyKeys(listOf(key))
    private infix fun S3ObjectInBucket.shouldHaveContent(text: String) =
        this.bucket.readText(this.s3Object) shouldBe text

    private infix fun S3Object.shouldBe(expectedResult: ExpectedResult) {
        when (expectedResult) {
            CorrectS3Dir -> {
                this.key() shouldEndWith "/"
                this.size() shouldBe 0
            }
        }
    }

    private data class S3ObjectInBucket(val s3Object: S3Object, val bucket: Bucket)

    private enum class ExpectedResult {
        CorrectS3Dir,
    }
}
