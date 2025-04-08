package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object AddToPath : BuildType({
    name = "AddToPath"

    vcs { root(RecipesVscRoot) }

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

    enableVcsTrigger()
})