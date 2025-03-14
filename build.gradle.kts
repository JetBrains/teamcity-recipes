plugins {
    id("com.jetbrains.teamcity-recipes")
}

recipes {
    setOutputPath(".teamcity/pluginData/_Self/metaRunners/")

    add("src/add-to-path")
    add("src/aws/install-aws-cli")
    add("src/build-godot-game")
    add("src/create-file")
    add("src/download-file")
    add("src/import-xml-report")
    add("src/pin-build")
    add("src/publish-artifacts")
    add("src/send-email")
    add("src/send-slack-message")
    add("src/set-build-status")
    add("src/set-environment-variable")
    add("src/setup-node")
    add("src/tag-build")
    add("src/tag-current-build")
    add("src/untag-current-build")
}
