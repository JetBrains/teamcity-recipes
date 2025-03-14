pluginManagement {
    includeBuild("recipes-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "teamcity-recipes"

include("recipes-tests")
