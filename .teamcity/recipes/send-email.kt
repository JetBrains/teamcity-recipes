package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object SendEmail : BuildType({
    name = "SendEmail"

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
})