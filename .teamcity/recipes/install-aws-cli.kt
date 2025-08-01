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
            id = "simpleRunner"
            name = "Check AWS CLI installation"
            scriptContent = "aws --version"
        }
    }

    enableVcsTrigger()
})