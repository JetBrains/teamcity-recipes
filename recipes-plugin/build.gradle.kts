plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("teamcityRecipes") {
            id = "com.jetbrains.teamcity-recipes"
            implementationClass = "RecipesPlugin"
        }
    }
}
