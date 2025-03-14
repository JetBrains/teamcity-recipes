
@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_SET_PARAMETER

val message = ServiceMessage.asString(
    BUILD_SET_PARAMETER,
    mapOf(
        "name" to "env.${requiredInput("name")}",
        "value" to requiredInput("value")
    )
)
println(message)

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")