package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object InstallAwsCLI : BuildType({
    name = "InstallAwsCLI"

    enableMatrixBuild()

    steps {
        step {
            id = "InstallAwsCli"
            type = Recipes.InstallAwsCli
            input("aws_cli_version", "latest")
        }
        script {
            id = "simpleRunner"
            scriptContent = "aws help"
        }
    }
})