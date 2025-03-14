@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.PUBLISH_ARTIFACTS

val message = ServiceMessage.asString(PUBLISH_ARTIFACTS, requiredInput("path_to_publish"))
println(message)

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")