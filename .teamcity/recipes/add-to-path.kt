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
            input("path", "some-path1")
        }
        step {
            id = "AddToPath2"
            type = Recipes.AddToPath
            input("path", "some-path2")
        }
        kotlinScript {
            id = "kotlinScript"
            content = """System.getenv("PATH")"""
        }
    }

    enableVcsTrigger()
})