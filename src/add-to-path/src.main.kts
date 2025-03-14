@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_SET_PARAMETER
import java.io.File.pathSeparator

val valueToPrepend = requiredInput("path")
val message = ServiceMessage.asString(
    BUILD_SET_PARAMETER,
    mapOf(
        "name" to "env.PATH",
        "value" to valueToPrepend + pathSeparator + System.getenv("PATH")
    )
)
println(message)
println("PATH environment variable was updated: $valueToPrepend has been prepended.")

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")