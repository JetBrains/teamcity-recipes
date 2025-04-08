package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object CreateFileAndPublish : BuildType({
    name = "CreateFileAndPublish"

    vcs { root(RecipesVscRoot) }

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

    enableVcsTrigger()
})