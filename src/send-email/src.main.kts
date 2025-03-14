@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.NOTIFICATION

val message = ServiceMessage.asString(
    NOTIFICATION, mapOf(
        "notifier" to "email",
        "message" to requiredInput("message"),
        "subject" to requiredInput("subject"),
        "address" to requiredInput("address"),
    )
)
println(message)
println("""
    The message has been added to the queue and will be sent in the background. If the email is not received, check the following:

    1. Ensure the Email Notifier is enabled and the SMTP server is configured in Administration → Email Notifier.
    2. Verify that the 'Service message notifications' → 'Notifications limit' parameter is set to a value greater than zero.
    3. Messages with external URLs may be blocked. Update the 'Allowed hostnames' parameter in Email Notifier settings if necessary.
    4. Ensure the recipient email addresses match the 'Allowed addresses' in Email Notifier settings.

    If the issue persists, check the server logs for further details.
""".trimIndent())

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")