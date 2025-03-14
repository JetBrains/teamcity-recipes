package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object CreateFileAndPublish : BuildType({
    name = "CreateFileAndPublish"

    enableMatrixBuild()

    steps {
        step {
            id = "CreateFile"
            type = Recipes.CreateFile
            input("path", "./created/secret.txt")
            input("content", "42")
        }
        step {
            id = "PublishArtifacts"
            type = Recipes.PublishArtifacts
            input("path_to_publish", "./created/")
        }
    }
})