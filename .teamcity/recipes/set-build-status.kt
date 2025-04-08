package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object SetBuildStatus : BuildType({
    name = "SetBuildStatus"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "SetBuildStatus"
            type = Recipes.SetBuildStatus
            input("status", "SUCCESS")
            input("text", "Custom success message")
        }
    }

    enableVcsTrigger()
})