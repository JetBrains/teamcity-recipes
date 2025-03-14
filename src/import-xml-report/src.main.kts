@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage

val message = ServiceMessage.asString("importData", mapOf(
    "type" to requiredInput("report_type"),
    "path" to requiredInput("import_path"),
))
println(message)

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")