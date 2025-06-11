package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object PinBuild : BuildType({
    name = "PinBuild"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "PinBuild"
            type = Recipes.PinBuild
            input("comment", "Pinning important release")
            input("build_id", "1187075")
            input("server_url", "%teamcity.serverUrl%")
            password("env.input_access_token", "credentialsJSON:19937da4-cff7-4261-952b-b25d9f8c5974")
        }
    }

    enableVcsTrigger()
})

