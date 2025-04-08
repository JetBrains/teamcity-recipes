package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object TagCurrentBuild : BuildType({
    name = "TagCurrentBuild"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "TagCurrentBuild"
            type = Recipes.TagCurrentBuild
            val tags = """
                Tag1
                Tag2
                """.trimIndent()
            input("tags", tags)
        }
    }

    enableVcsTrigger()
})