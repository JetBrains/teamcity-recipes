package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript

object SetEnvironmentVariable : BuildType({
    name = "SetEnvironmentVariable"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "SetEnvironmentVariable"
            type = Recipes.SetEnvironmentVariable
            input("name", "TEST_ENV_KEY")
            input("value", "TEST_ENV_VALUE")
        }
        kotlinScript {
            id = "kotlinScript"
            content = """System.getenv("TEST_ENV_KEY")"""
        }
    }

    enableVcsTrigger()
})