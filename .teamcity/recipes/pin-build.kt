package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object PinBuild : BuildType({
    name = "PinBuild"

    enableMatrixBuild()

    steps {
        step {
            id = "PinBuild"
            type = Recipes.PinBuild
            input("comment", "Pinning important release")
            input("build_id", "1187075")
            input("server_url", "%teamcity.serverUrl%")
            input("access_token", "credentialsJSON:1707ebb7-f822-401e-bc54-337c32c3f029")
        }
    }
})

