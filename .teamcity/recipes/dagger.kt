package recipes

import Recipes
import RecipesVscRoot
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix

object Dagger : BuildType({
    name = "Dagger"

    vcs { root(RecipesVscRoot) }

    requirements {
        exists("docker.server.version")
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        step {
            id = "DaggerCommand"
            name = "Install Dagger and run a simple command"
            type = Recipes.Dagger
            input("version", "0.18.2")
            input("command", "dagger init --name test-module")
            input("stop_engine", "false")
            input("installation_path", """%teamcity.agent.tools.dir%""")
        }
        script {
            id = "cmd"
            name = "Call dagger from command line step"
            scriptContent = "\$DAGGER_EXEC version"
        }
        step {
            id = "StopDagger"
            name = "Stop Dagger engine"
            type = Recipes.Dagger
            input("version", "0.18.2")
            input("installation_path", """%teamcity.agent.tools.dir%""")
        }
    }

    enableVcsTrigger()
})