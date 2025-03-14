package recipes

import Recipes
import enableMatrixBuild
import input
import jetbrains.buildServer.configs.kotlin.BuildType

object SendSlackMessage : BuildType({
    name = "SendSlackMessage"

    enableMatrixBuild()

    steps {
        step {
            id = "SendSlackMessage"
            type = Recipes.SendSlackMessage
            input("send_to", "U05NQV1P03S")
            input("connection_id", "PROJECT_EXT_486")
            input("message", "Hello from recipe")
        }
    }
})