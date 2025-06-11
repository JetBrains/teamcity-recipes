package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object SetupBazelisk : BuildType({
    name = "SetupBazelisk"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "SetupBazelisk"
            type = Recipes.SetupBazelisk
            input("version", "1.26")
            input("installation_path", ".")
        }
        script {
            id = "simpleRunnerShell"
            name = "Check Bazelisk installation on Linux/Mac"
            conditions { doesNotContain("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "\$BAZELISK_EXEC --version"
        }
        script {
            id = "simpleRunnerWindows"
            name = "Check Bazelisk installation on Windows"
            conditions { contains("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "%%BAZELISK_EXEC%% --version"
        }
        // install again to check it won't install again
        step {
            id = "SetupBazelisk2"
            type = Recipes.SetupBazelisk
            input("version", "1.26")
            input("installation_path", ".")
        }
        script {
            id = "simpleRunnerShell2"
            name = "Check Bazelisk installation on Linux/Mac"
            conditions { doesNotContain("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "\$BAZELISK_EXEC --version"
        }
        script {
            id = "simpleRunnerWindows2"
            name = "Check Bazelisk installation on Windows"
            conditions { contains("teamcity.agent.jvm.os.name", "Windows") }
            scriptContent = "%%BAZELISK_EXEC%% --version"
        }
    }

    enableVcsTrigger()
})