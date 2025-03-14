@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.NOTIFICATION

val message = ServiceMessage.asString(
    NOTIFICATION, mapOf(
        "notifier" to "slack",
        "message" to requiredInput("message"),
        "sendTo" to requiredInput("send_to"),
        "connectionId" to requiredInput("connection_id"),
    )
)
println(message)
println("""
    The message has been added to the queue and will be sent in the background. If the notification is not received, check the following:
    
    1. Ensure the 'Service message notifications' → 'Notifications limit' in the Slack connections settings is set to a value greater than zero.
    2. Verify the Channel ID or User ID is correct.
    
    If the issue persists, check the server logs for further details.
""".trimIndent())

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")