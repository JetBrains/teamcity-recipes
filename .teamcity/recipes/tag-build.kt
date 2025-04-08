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
            input("access_token", "credentialsJSON:1707ebb7-f822-401e-bc54-337c32c3f029")
        }
    }

    enableVcsTrigger()
})