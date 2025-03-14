package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object UntagCurrentBuild : BuildType({
    name = "UntagCurrentBuild"

    enableMatrixBuild()

    steps {
        step {
            id = "TagCurrentBuild"
            type = Recipes.TagCurrentBuild
            input(
                "tags", """
                Tag1
                Tag2
                """.trimIndent()
            )
        }
        step {
            id = "UntagCurrentBuild"
            type = Recipes.UntagCurrentBuild
            input(
                "tags", """
                Tag1
                Tag2
                """.trimIndent()
            )
        }
    }
})