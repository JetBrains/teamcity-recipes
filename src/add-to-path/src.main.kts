@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_SET_PARAMETER

val valueToPrepend = requiredInput("path")

private fun addToPath(newPath: String) {
    val updated = System.getenv("TEAMCITY_PREPEND_PATH").let {
        if (it.isNullOrBlank()) {
            newPath
        } else {
            "$newPath\n$it"
        }
    }

    val message = asString(
        BUILD_SET_PARAMETER, mapOf<String, String>("name" to "env.TEAMCITY_PREPEND_PATH", "value" to updated)
    )
    println(message)
}
addToPath(valueToPrepend)
println("PATH environment variable was updated: $valueToPrepend has been prepended.")

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")