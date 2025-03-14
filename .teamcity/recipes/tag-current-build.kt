package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object TagCurrentBuild : BuildType({
    name = "TagCurrentBuild"

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
})