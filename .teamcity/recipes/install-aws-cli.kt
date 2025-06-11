package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object InstallAwsCLI : BuildType({
    name = "InstallAwsCLI"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "InstallAwsCli"
            type = Recipes.InstallAwsCli
            input("aws_cli_version", "latest")
        }
        script {
            id = "simpleRunnerShell"
            name = "Check AWS CLI installation on Linux/Mac"
            conditions { doesNotContain("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "\$AWS_CLI_EXEC --version"
        }
        script {
            id = "simpleRunnerWindows"
            name = "Check AWS CLI installation on Windows"
            conditions { contains("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "%%AWS_CLI_EXEC%% --version"
        }
    }

    enableVcsTrigger()
})