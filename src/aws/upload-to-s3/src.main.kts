@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("software.amazon.awssdk:s3:2.30.38")
@file:DependsOn("software.amazon.awssdk:auth:2.30.38")

@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI
import java.nio.file.*
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*
import kotlin.system.exitProcess

runCatchingWithLogging {
    val source = requiredInput("source", errorMessage = "Source path is missing")
    val target = requiredInput("target", errorMessage = "Target path is missing")

    val sourcePath = parseSource(source)
    val (bucket, s3Key) = parseTarget(target)

    uploadToS3(sourcePath, bucket, s3Key)
}

fun uploadToS3(source: Path, bucket: String, s3Key: String) {
    with(createS3Client()) {
        verifyBucket(bucket)
        when {
            source.isRegularFile() -> uploadFile(source, bucket, s3Key)
            source.isDirectory() -> uploadDirectory(source, bucket, s3Key)
            else -> error("$source exists but has an unknown type, only files and directories are supported")
        }
    }
}

fun S3Client.uploadFile(path: Path, bucket: String, s3KeyInput: String) {
    val targetIsBucketRoot = s3KeyInput.isBlank()
    val targetIsDirectory = { s3KeyInput.endsWith("/") || objectExists(bucket, "$s3KeyInput/") }
    val s3Key = when {
        targetIsBucketRoot -> path.name
        targetIsDirectory() -> s3KeyInput.ensureTrailingSlash() + path.name
        else -> s3KeyInput
    }
    val logFromTo = "$path to s3://$bucket/$s3Key"
    try {
        writeDebug("Uploading $logFromTo")
        putObject(PutObjectRequest.builder().bucket(bucket).key(s3Key).build(), path)
        println("Successfully uploaded $logFromTo")
    } catch (e: Throwable) {
        writeError("Failed to upload $logFromTo")
        throw e
    }
}

fun S3Client.uploadDirectory(path: Path, bucket: String, s3KeyInput: String) {
    if (path.listDirectoryEntries().isEmpty()) {
        println("Directory at $path is empty, nothing to upload")
        return
    }
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val subKey = s3KeyInput.ensureTrailingSlash() + file.relativeTo(path).ensureForwardSlashes()
            uploadFile(file, bucket, subKey.removeSuffix(file.name))
            return CONTINUE
        }

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (dir.listDirectoryEntries().isEmpty()) {
                val subKey = s3KeyInput.ensureTrailingSlash() + dir.relativeTo(path).ensureForwardSlashes()
                uploadEmptyDir(bucket, "$subKey/")
            }
            return CONTINUE
        }
    })
}

fun S3Client.uploadEmptyDir(bucket: String, key: String) {
    writeDebug("Creating folder at s3://$bucket/$key")
    putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(),
        RequestBody.fromBytes(ByteArray(0)),
    )
    println("Successfully created folder at s3://$bucket/$key")
}

fun createS3Client(): S3Client {
    val s3Builder = S3Client.builder()
    val awsEndpoint = System.getenv("AWS_ENDPOINT")
    if (!awsEndpoint.isNullOrEmpty()) {
        s3Builder
            .endpointOverride(URI.create(awsEndpoint))
            .serviceConfiguration { it.pathStyleAccessEnabled(true) }
    }
    return s3Builder
        .credentialsProvider(createCredentialsProvider())
        .withAwsRegion()
        .build()
}

fun createCredentialsProvider(): AwsCredentialsProvider {
    val accessKeyInput = input("aws_access_key_id")
    val secretKeyInput = input("aws_secret_access_key")
    if (accessKeyInput != null && secretKeyInput != null) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyInput, secretKeyInput))
    }
    return DefaultCredentialsProvider.create()
}

fun S3ClientBuilder.withAwsRegion(): S3ClientBuilder {
    input("aws_region")?.let { return region(Region.of(it)) }
    System.getenv("AWS_REGION")?.let { return region(Region.of(it)) }
    System.getenv("AWS_DEFAULT_REGION")?.let { return region(Region.of(it)) }
    return this
}

fun S3Client.objectExists(bucket: String, key: String): Boolean {
    return try {
        headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
        true
    } catch (e: Exception) {
        false
    }
}

fun S3Client.verifyBucket(bucket: String) {
    try {
        headBucket(HeadBucketRequest.builder().bucket(bucket).build())
    } catch (e: S3Exception) {
        val region = serviceClientConfiguration().region().id()
        writeError(
            """
            Failed to fetch info about "$bucket" bucket in "$region" AWS region.
            Please verify the bucket name, ensure the correct AWS region is configured, and check the AWS credentials and permissions.
            Switch to "Verbose" log level to see stacktrace 
            """.trimIndent()
        )
        writeDebug(e.stackTraceToString())
        exitProcess(1)
    }
}

fun parseSource(path: String): Path =
    Paths.get(path).normalize().also { if (!it.exists()) error("File or directory does not exist at $path") }

fun parseTarget(path: String): Pair<String, String> {
    val bucket = path
        .removePrefix("s3://")
        .substringBefore("/")
    if (bucket.isEmpty()) {
        error("Missing S3 bucket in $path")
    }
    val key = path
        .removePrefix("s3://$bucket")
        .removePrefix("/")
    return bucket to key
}

fun Path.ensureForwardSlashes(): String = toString().replace("\\", "/")
fun String.ensureTrailingSlash() = if (this.isBlank()) this else removeSuffix("/").removeSuffix("\\") + "/"

fun input(name: String): String? = System.getenv("input_$name")
fun requiredInput(name: String, errorMessage: String) = System.getenv("input_$name") ?: error(errorMessage)
fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
    println(asString(MESSAGE, mapOf("text" to text, *attributes)))

fun writeError(text: String) = writeMessage(text, "status" to "ERROR")
fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    writeError("""${it.message} (Switch to "Verbose" log level to see stacktrace)""")
    writeDebug(it.stackTraceToString())
    exitProcess(1)
}
