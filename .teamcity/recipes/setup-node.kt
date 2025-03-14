package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object SetupNode : BuildType({
    name = "SetupNode"

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
            scriptContent = "node --version"
        }
    }
})