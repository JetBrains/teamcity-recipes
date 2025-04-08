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
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.streams.asSequence
import kotlin.system.exitProcess

runCatchingWithLogging {
    val source = requiredInput("source", errorMessage = "Source path is missing")
    val target = requiredInput("target", errorMessage = "Target path is missing")

    val sourceRoot = Paths.get(source).normalize()
    if (sourceRoot.isDirectory() && sourceRoot.listDirectoryEntries().isEmpty()) {
        println("Directory at $sourceRoot is empty, nothing to upload")
        exitProcess(0)
    }
    val sourcePaths = findMatchingPaths(sourceRoot.pathString)
        .filter { it.isRegularFile() || (it.isDirectory() && it.listDirectoryEntries().isEmpty()) }
        .onEach { checkPathAllowed(it) }
        .toList()
    val (bucket, s3Key) = parseTarget(target)

    val basePath = findBasePathWithoutWildcards(sourceRoot.pathString)
    with(createS3Client()) {
        verifyBucket(bucket)
        sourcePaths.forEach { uploadToS3(basePath, it, bucket, s3Key) }
    }
}

fun S3Client.uploadToS3(basePath: Path, path: Path, bucket: String, s3KeyInput: String) {
    val targetIsBucketRoot = s3KeyInput.isBlank()
    val targetIsDirectory = {
        s3KeyInput.endsWith("/") || basePath.isDirectory() || objectExists(bucket, "$s3KeyInput/")
    }
    val relativePath = if (basePath.isDirectory()) path.relativeTo(basePath) else path.fileName
    val s3Key = when {
        targetIsBucketRoot -> relativePath.ensureForwardSlashes()
        targetIsDirectory() -> s3KeyInput.ensureTrailingSlash() + relativePath.ensureForwardSlashes()
        else -> s3KeyInput
    }
    val logFromTo = "${path.absolutePathString()} to s3://$bucket/$s3Key"
    try {
        writeDebug("Uploading $logFromTo")
        if (path.isDirectory() && path.listDirectoryEntries().isEmpty()) {
            uploadEmptyDir(bucket, "$s3Key/")
        } else {
            putObject(PutObjectRequest.builder().bucket(bucket).key(s3Key).build(), path)
        }
        println("Successfully uploaded $logFromTo")
    } catch (e: Throwable) {
        writeError("Failed to upload $logFromTo")
        throw e
    }
}

fun S3Client.uploadEmptyDir(bucket: String, key: String) {
    putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(),
        RequestBody.fromBytes(ByteArray(0)),
    )
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

fun findMatchingPaths(globPattern: String): Sequence<Path> = sequence {
    val visited = mutableSetOf<Path>()
    when {
        containsWildcard(globPattern) -> {
            val basePath = findBasePathWithoutWildcards(globPattern)
            val shouldAddDotSlash = !Paths.get(globPattern).isAbsolute && !globPattern.startsWith(".")
            val pattern = if (shouldAddDotSlash) "./$globPattern" else globPattern
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
            Files.walk(basePath).asSequence().forEach { path ->
                if (matcher.matches(path) && !visited.contains(path)) {
                    visited.add(path); yield(path)
                    if (path.isDirectory()) {
                        // if dir matches the pattern all its content should be included
                        Files.walk(path).asSequence().forEach {
                            if (!visited.contains(it)) {
                                visited.add(it); yield(it)
                            }
                        }
                    }
                }
            }
        }

        else -> {
            val path = Paths.get(globPattern)
            when {
                path.isRegularFile() -> yield(path)
                path.isDirectory() -> Files.walk(path).asSequence().forEach { yield(it) }
                else -> error("File or directory does not exist at $path")
            }
        }
    }
}

fun containsWildcard(path: String): Boolean {
    return listOf("*", "**", "?").any { path.contains(it) }
}

fun findBasePathWithoutWildcards(path: String): Path {
    val stableSegments = splitPathToSegments(path).takeWhile { !containsWildcard(it) }
    return if (stableSegments.isEmpty()) {
        Paths.get(".")
    } else {
        Paths.get("", *stableSegments.toTypedArray())
    }
}

fun splitPathToSegments(sourcePath: String): List<String> {
    val path = Paths.get(sourcePath)
    val segments = mutableListOf<String>()
    path.root?.toString()?.let { segments.add(it) }
    val pathIter = path.iterator()
    while (pathIter.hasNext()) segments.add(pathIter.next().toString())
    return segments
}

fun checkPathAllowed(sourcePath: Path) {
    val allowedDirs = listOfNotNull(
        """%teamcity.build.checkoutDir%""",
        """%teamcity.build.workingDir%""",
        System.getenv("TMPDIR"), // points to buildTmp
    )
    val path = sourcePath.toRealPath()
    val isPathAllowed = allowedDirs.any { allowedDir -> path.startsWith(Paths.get(allowedDir).toRealPath()) }
    if (!isPathAllowed) {
        error("$sourcePath source path is not permitted. Allowed locations: $allowedDirs")
    }
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

