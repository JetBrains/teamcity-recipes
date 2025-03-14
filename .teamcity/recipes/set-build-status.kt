package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object SetBuildStatus : BuildType({
    name = "SetBuildStatus"

    enableMatrixBuild()

    steps {
        step {
            id = "SetBuildStatus"
            type = Recipes.SetBuildStatus
            input("status", "SUCCESS")
            input("text", "Custom success message")
        }
    }
})