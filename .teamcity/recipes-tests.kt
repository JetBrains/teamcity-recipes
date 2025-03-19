import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.RelativeId
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object RecipesTests : BuildType({
    name = "Recipes Tests"

    vcs {
        root(RelativeId("TeamCity_Sandbox_TeamcityRecipes_SshGitGitJetbrainsTeamTeamcityBuildToolsIntegrationsTeamcityRecipesGitRefsHeadsMain"))
    }

    steps {
        gradle {
            name = "Run Tests"
            tasks = "test"
            useGradleWrapper = true
        }
    }

    triggers {
        vcs {
            branchFilter = "+:<default>"
        }
    }
})
