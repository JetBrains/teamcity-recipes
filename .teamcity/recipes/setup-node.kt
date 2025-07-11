package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object SetupNode : BuildType({
    name = "SetupNode"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "SetupNode"
            type = Recipes.SetupNode
            input("version", "23")
            input("installation_path", ".")
        }
        script {
            id = "simpleRunner"
            name = "Verify Node installation"
            scriptContent = "node --version"
        }
        // install again to check it won't install again
        step {
            id = "SetupNode2"
            type = Recipes.SetupNode
            input("version", "23")
            input("installation_path", ".")
        }
        script {
            id = "simpleRunner2"
            name = "Verify Node installation"
            scriptContent = "node --version"
        }
    }

    enableVcsTrigger()
})