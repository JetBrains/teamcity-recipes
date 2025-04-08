import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object RecipesVscRoot : GitVcsRoot({
    name = "teamcity-recipes-github"
    url = "https://github.com/JetBrains/teamcity-recipes.git"
    branchSpec = "+:refs/heads/*"
    branch = "refs/heads/main"
})