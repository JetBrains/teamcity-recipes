@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.BUILD_STATUS

val message = ServiceMessage.asString(
    BUILD_STATUS,
    mapOf(
        "text" to requiredInput("text"),
        "status" to requiredInput("status"),
    )
)
println(message)

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")
