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

    params {
        password("access_token", "credentialsJSON:75547608-9b76-4bdb-a21e-d2ad8919cd22")
    }

    steps {
        step {
            id = "PinBuild"
            type = Recipes.PinBuild
            input("comment", "Pinning important release")
            input("build_id", "1187075")
            input("server_url", "%teamcity.serverUrl%")
            input("access_token", "%access_token%")
        }
    }

    enableVcsTrigger()
})

