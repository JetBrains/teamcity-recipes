import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version
import recipes.*

version = "2024.12"

project {
    vcsRoot(RecipesVscRoot)

    buildType(RecipesTests)

    buildType(AddToPath)
    buildType(AwsCliDocker)
    buildType(CreateFileAndPublish)
    buildType(Dagger)
    buildType(DownloadFileAndPublish)
    buildType(ImportJUnitReport)
    buildType(InstallAwsCLI)
    buildType(PinBuild)
    buildType(SendEmail)
    buildType(SendSlackMessage)
    buildType(SetBuildStatus)
    buildType(SetEnvironmentVariable)
    buildType(SetupBazelisk)
    buildType(SetupNode)
    buildType(TagBuild)
    buildType(TagCurrentBuild)
    buildType(UntagCurrentBuild)
}
