@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.ADD_BUILD_TAG

val tags = requiredInput("tags")
tags.lines().filter { it.isNotEmpty() }.forEach {
    println(ServiceMessage.asString(ADD_BUILD_TAG, it))
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")