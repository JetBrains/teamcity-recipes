import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version
import recipes.*

version = "2024.12"

project {
    buildType(AddToPath)
    buildType(CreateFileAndPublish)
    buildType(DownloadFileAndPublish)
    buildType(ImportJUnitReport)
    buildType(InstallAwsCLI)
    buildType(PinBuild)
    buildType(SendEmail)
    buildType(SendSlackMessage)
    buildType(SetBuildStatus)
    buildType(SetEnvironmentVariable)
    buildType(SetupNode)
    buildType(TagBuild)
    buildType(TagCurrentBuild)
    buildType(UntagCurrentBuild)
}

