package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript


object UploadToS3 : BuildType({
    name = "UploadToS3"

    enableMatrixBuild()

    params {
        param("env.AWS_ACCESS_KEY_ID", "credentialsJSON:055e57bc-d34c-4a36-9e2f-8ccbb19d8e50")
        param("env.AWS_SECRET_ACCESS_KEY", "credentialsJSON:6b27bf03-ac42-4fc8-8aff-ffb911fc723c")
        param("env.AWS_REGION", "eu-north-1")

        param("pathToUploadFrom", "./created/file.txt")
        param("pathToUploadTo", "s3://teamcity-recipe-test/file.txt")
        param("pathToDownloadTo", "./created/downloaded.txt")
        param("fileContent", "42")
    }

    steps {
        step {
            id = "CreateFile"
            type = Recipes.CreateFile
            input("path", "%pathToUploadFrom%")
            input("content", "%fileContent%")
        }
        step {
            id = "UploadToS3"
            type = Recipes.UploadToAwsS3
            input("source", "%pathToUploadFrom%")
            input("target", "%pathToUploadTo%")
        }
        step {
            id = "DownloadFromS3"
            type = Recipes.DownloadFromAwsS3
            input("source", "%pathToUploadTo%")
            input("target", "%pathToDownloadTo%")
        }
        kotlinScript {
            id = "kotlinScript"
            content = """
                import java.nio.file.Files
                import java.nio.file.Path
                import java.nio.file.Paths
                
                fun main() {
                    val filePath: Path = Paths.get("%pathToDownloadTo%")
                
                    if (!Files.exists(filePath)) error("Downloaded file is not found")
                    val content = Files.readString(filePath)
                    if (content != "%fileContent%") error("Expected '%fileContent%' but got '$content'")
                }
                """.trimIndent()
        }
    }
})
