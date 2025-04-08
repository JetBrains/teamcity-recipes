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
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.system.exitProcess

runCatchingWithLogging {
    val source = requiredInput("source", errorMessage = "Source path is missing")
    val target = requiredInput("target", errorMessage = "Target path is missing")

    val (bucket, s3Key) = parseSource(source)
    downloadFromS3(bucket, s3Key, target)
}

fun downloadFromS3(bucket: String, s3Key: String, target: String) {
    with(createS3Client()) {
        verifyBucket(bucket)
        val s3Objects = listObjects(bucket, s3Key).contents()
        val targetPath = Paths.get(target).normalize()
        when {
            s3Objects.isEmpty() -> error("S3 object not found at s3://$bucket/$s3Key")
            s3Objects.size == 1 -> {
                val targetFile = when {
                    target.endsWith("/") || targetPath.isDirectory() -> {
                        val fileName = s3Key.substringAfterLast("/")
                        targetPath.resolve(fileName)
                    }

                    else -> targetPath
                }.also { it.createParentDirectories() }
                downloadFile(bucket, s3Key, targetFile)
            }

            else -> downloadDir(bucket, s3Key, s3Objects, targetPath)
        }
    }
}

fun S3Client.downloadDir(bucket: String, s3KeyInput: String, s3Objects: List<S3Object>, target: Path) {
    val targetDir: Path = when {
        !target.exists() -> target.createDirectories()
        target.isDirectory() -> target
        target.isRegularFile() -> error(
            "Failed to download contents from s3://$bucket/$s3KeyInput to $target. The target is an existing file"
        )

        else -> error("$target exists but has an unknown type, only files and directories are supported")
    }
    s3Objects.forEach { s3Object ->
        val s3Key = s3Object.key()
        val relativePath = s3Key.removePrefix(s3KeyInput).removePrefix("/")
        val targetFile = targetDir.resolve(relativePath).also { it.createParentDirectories() }
        val isEmptyDirInS3 = s3Key.endsWith("/") && s3Object.size() == 0L
        if (isEmptyDirInS3) {
            Files.createDirectories(targetFile)
            println("Created an empty directory at ${targetFile.absolutePathString()} to match s3://$bucket/$s3Key")
        } else {
            downloadFile(bucket, s3Key, targetFile)
        }
    }
}

fun S3Client.downloadFile(bucket: String, key: String, targetFile: Path) {
    val logFromTo = "s3://$bucket/$key to $targetFile"
    try {
        writeDebug("Downloading $logFromTo")
        if (targetFile.exists()) {
            writeDebug("Deleting existing $targetFile")
            targetFile.deleteExisting()
        }
        getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(), targetFile)
        println("Successfully downloaded $logFromTo")
    } catch (e: Throwable) {
        writeError("Failed to download $logFromTo")
        throw e
    }
}

fun createS3Client(): S3Client {
    val s3Builder = S3Client.builder()
    val minIoEndpoint = System.getenv("MIN_IO_ENDPOINT")
    if (!minIoEndpoint.isNullOrEmpty()) {
        s3Builder
            // MinIO requires these settings
            .endpointOverride(URI.create(minIoEndpoint))
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
    return when {
        !accessKeyInput.isNullOrBlank() && !secretKeyInput.isNullOrBlank() ->
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyInput, secretKeyInput))

        !accessKeyInput.isNullOrBlank() || !secretKeyInput.isNullOrBlank() ->
            error("Both access key and secret key inputs should be provided or left empty")

        else -> DefaultCredentialsProvider.create()
    }
}

fun S3ClientBuilder.withAwsRegion(): S3ClientBuilder {
    input("aws_region")?.let { if (it.isNotEmpty()) return region(Region.of(it)) }
    System.getenv("AWS_REGION")?.let { if (it.isNotEmpty()) return region(Region.of(it)) }
    System.getenv("AWS_DEFAULT_REGION")?.let { if (it.isNotEmpty()) return region(Region.of(it)) }
    return this
}

fun S3Client.listObjects(bucket: String, prefix: String): ListObjectsV2Response {
    return listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
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

fun parseSource(path: String): Pair<String, String> {
    if (!path.startsWith("s3://")) {
        error("Source should be a path to S3 object in \"s3://bucket/key\" format")
    }
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

fun input(name: String): String? = System.getenv("input_$name")
fun requiredInput(name: String, errorMessage: String) = input(name) ?: error(errorMessage)
fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
    println(asString(MESSAGE, mapOf("text" to text, *attributes)))

fun writeError(text: String) = writeMessage(text, "status" to "ERROR")
fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    writeError("""${it.message} (Switch to "Verbose" log level to see stacktrace)""")
    writeDebug(it.stackTraceToString())
    exitProcess(1)
}
