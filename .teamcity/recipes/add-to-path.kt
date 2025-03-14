package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object AddToPath : BuildType({
    name = "AddToPath"

    enableMatrixBuild()

    steps {
        step {
            id = "AddToPath"
            type = Recipes.AddToPath
            input("path", "some-path")
        }
        kotlinScript {
            id = "kotlinScript"
            content = """System.getenv("PATH")"""
        }
    }
})