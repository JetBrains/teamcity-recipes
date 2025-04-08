package aws

import RecipeTest
import io.kotest.core.spec.Spec
import org.testcontainers.containers.MinIOContainer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.name
import kotlin.io.path.readText

abstract class AwsS3Test : RecipeTest() {

    protected lateinit var s3Client: S3Client

    protected val user = "minioadmin"
    protected val password = "minioadmin"

    private var container: MinIOContainer = MinIOContainer("minio/minio")
        .withUserName(user)
        .withPassword(password)

    protected val region = Region.EU_WEST_3
    protected val endpoint: String
        get() = "http://localhost:${container.getMappedPort(9000)}"

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        container.start()
        val credentials = AwsBasicCredentials.create(user, password)
        s3Client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .serviceConfiguration { it.pathStyleAccessEnabled(true) }
            .endpointOverride(URI.create(endpoint))
            .region(region)
            .build()
    }

    override fun afterSpec(f: suspend (Spec) -> Unit) {
        super.afterSpec(f)
        s3Client.close()
        container.stop()
    }

    override fun readScript() = super.readScript().withNewEnv(
        mapOf(
            "AWS_REGION" to region.id(),
            "AWS_ACCESS_KEY_ID" to user,
            "AWS_SECRET_ACCESS_KEY" to password,
            "MIN_IO_ENDPOINT" to endpoint,
        )
    )

    protected fun Bucket.readText(s3Object: S3Object): String {
        val tmpFile = tempFile().also { it.deleteExisting() }
        s3Client.getObject(GetObjectRequest.builder().bucket(this.name).key(s3Object.key()).build(), tmpFile)
        return tmpFile.readText()
    }

    protected fun Bucket.listAllObjects(): List<S3Object> {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(this.name).build()).contents()
    }

    protected fun Bucket.putObject(file: Path, key: String = file.name) {
        s3Client.putObject(PutObjectRequest.builder().bucket(this.name).key(key).build(), file)
    }

    protected fun Bucket.createEmptyDir(dirKey: String) {
        val key = if (dirKey.endsWith("/")) dirKey else "$dirKey/"
        s3Client.putObject(
            PutObjectRequest.builder().bucket(this.name).key(key).build(),
            RequestBody.fromBytes(ByteArray(0)),
        )
    }

    protected fun Bucket.deleteAllObjects() {
        val objects = listAllObjects()
        if (objects.isEmpty()) {
            return
        }
        val keys = objects.map { ObjectIdentifier.builder().key(it.key()).build() }
        val deleteRequest = DeleteObjectsRequest.builder().bucket(this.name)
            .delete(Delete.builder().objects(keys).build())
            .build()
        s3Client.deleteObjects(deleteRequest)
    }

    @JvmInline
    protected value class Bucket(val name: String) {
        override fun toString() = name
    }
}
