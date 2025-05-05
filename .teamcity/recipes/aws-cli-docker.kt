package recipes

import Recipes
import RecipesVscRoot
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object AwsCliDocker : BuildType({
    name = "AwsCliDocker"

    vcs { root(RecipesVscRoot) }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    params {
        password("env.AWS_ACCESS_KEY_ID", "credentialsJSON:9dfe3fc1-e2d7-407d-a547-1c9f34927a1f")
        password("env.AWS_SECRET_ACCESS_KEY", "credentialsJSON:1ba0e9a6-5d52-4dd1-87b8-2dc4b505061a")
        param("env.AWS_REGION", "eu-north-1")

        param("pathToUploadFrom", "./created/file.txt")
        param("pathToUploadTo", "s3://teamcity-recipe-test/file.txt")
        param("pathToDownloadTo", "./created/downloaded.txt")
        param("fileContent", "42")
    }

    steps {
        step {
            id = "CreateFile"
            name = "Create file for tests"
            type = Recipes.CreateFile
            input("path", "%pathToUploadFrom%")
            input("content", "%fileContent%")
        }
        step {
            id = "UploadToS3"
            name = "Upload file to S3 using recipe"
            type = Recipes.AwsCliDocker
            input("command", "aws s3 cp %pathToUploadFrom% %pathToUploadTo%")
        }
        step {
            id = "DownloadFromS3"
            name = "Download file from S3 using recipe"
            type = Recipes.AwsCliDocker
            input("command", "aws s3 cp %pathToUploadTo% %pathToDownloadTo%")
        }
        kotlinScript {
            id = "kotlinScript"
            name = "Check that file has been downloaded"
            content = """
                import java.nio.file.Files
                import java.nio.file.Path
                import java.nio.file.Paths
                import java.nio.charset.StandardCharsets
                
                fun main() {
                    val filePath: Path = Paths.get("%pathToDownloadTo%")
                
                    if (!Files.exists(filePath)) error("Downloaded file is not found")
                    val content = String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)
                    if (content != "%fileContent%") error("Expected '%fileContent%' but got '$content'")
                }
                """.trimIndent()
        }
    }

    enableVcsTrigger()
})
