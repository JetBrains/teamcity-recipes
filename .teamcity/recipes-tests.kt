import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object RecipesTests : BuildType({
    name = "Recipes Tests"

    vcs { root(RecipesVscRoot) }

    requirements {
        equals("container.engine", "docker")
        equals("container.engine.osType", "linux")
    }

    steps {
        gradle {
            name = "Run Tests"
            tasks = "test"
            useGradleWrapper = true
        }
    }

    enableVcsTrigger()
})
