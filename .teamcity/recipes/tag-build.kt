package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object TagBuild : BuildType({
    name = "TagBuild"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    params {
        password("access_token", "credentialsJSON:75547608-9b76-4bdb-a21e-d2ad8919cd22")
    }

    steps {
        step {
            id = "TagBuild"
            type = Recipes.TagBuild
            val tags = """
                Tag1
                Tag2
                """.trimIndent()
            input("tags", tags)
            input("build_id", "%teamcity.build.id%")
            input("server_url", "%teamcity.serverUrl%")
            input("access_token", "%access_token%")
        }
    }

    enableVcsTrigger()
})