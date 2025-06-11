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
            id = "simpleRunnerShell"
            name = "Check Node installation on Linux/Mac"
            conditions { doesNotContain("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "\$NODE_EXEC --version"
        }
        script {
            id = "simpleRunnerWindows"
            name = "Check Node installation on Windows"
            conditions { contains("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "%%NODE_EXEC%% --version"
        }
        // install again to check it won't install again
        step {
            id = "SetupNode2"
            type = Recipes.SetupNode
            input("version", "23")
            input("installation_path", ".")
        }
        script {
            id = "simpleRunnerShell2"
            name = "Check Node installation on Linux/Mac"
            conditions { doesNotContain("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "\$NODE_EXEC --version"
        }
        script {
            id = "simpleRunnerWindows2"
            name = "Check Node installation on Windows"
            conditions { contains("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "%%NODE_EXEC%% --version"
        }
    }

    enableVcsTrigger()
})