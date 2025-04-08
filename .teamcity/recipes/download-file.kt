package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object DownloadFileAndPublish : BuildType({
    name = "DownloadFileAndPublish"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "DownloadJsonFile"
            type = Recipes.DownloadFile
            input("output_dir", "./downloaded/")
            input("url", "https://nodejs.org/dist/index.json")
        }

        step {
            id = "DownloadFileBearerAuth"
            type = Recipes.DownloadFile
            input("url", "https://httpbin.org/bearer")
            input("output_dir", "./downloaded/")
            input("filename", "bearer-auth.json")
            input("bearer_token", "user")
        }

        step {
            id = "DownloadFileCustomHeaders"
            type = Recipes.DownloadFile
            input("url", "https://httpbin.org/headers")
            input("output_dir", "./downloaded/")
            input("filename", "headers.json")
            input(
                "headers", """
                |X-Secret-Header: X-Secret-Value
                |Second-Header: Second-Value
            """.trimMargin()
            )
        }

        step {
            id = "PublishArtifacts"
            type = Recipes.PublishArtifacts
            input("path_to_publish", "./downloaded/")
        }
    }

    enableVcsTrigger()
})