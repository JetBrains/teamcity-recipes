package recipes

import Recipes
import RecipesVscRoot
import enableMatrixBuild
import enableVcsTrigger
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object SendEmail : BuildType({
    name = "SendEmail"

    vcs { root(RecipesVscRoot) }

    enableMatrixBuild()

    steps {
        step {
            id = "SendEmail"
            type = Recipes.SendEmail
            input("address", "vladislav.ma-iu-shan@jetbrains.com")
            input("subject", "Recipe test")
            input("message", "Hello from recipe")
        }
    }

    enableVcsTrigger()
})