@file:Repository("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
@file:DependsOn("org.jetbrains.teamcity:teamcity-rest-client:3.0.3")
@file:Repository("https://download.jetbrains.com/teamcity-repository/")
@file:DependsOn("org.jetbrains.teamcity:serviceMessages:2024.12")
@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")

import kotlinx.coroutines.runBlocking
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.TeamCityInstanceBuilder
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.TAGS_ATRRIBUTE
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage.asString
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes.MESSAGE
import org.jetbrains.teamcity.rest.coroutines.TeamCityCoroutinesInstance

// by default teamcity-rest-client slf4j logger writes everything into stderr
System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")

runCatchingWithLogging {
    val server = getServerInstance()
    val buildId = requiredInput("build_id")
    runBlocking {
        server
            .build(BuildId(buildId))
            .pin(input("comment"))
    }
}

fun getServerInstance(): TeamCityCoroutinesInstance {
    val builder = TeamCityInstanceBuilder(requiredInput("server_url"))
    val accessToken = input("access_token")

    val server = when {
        accessToken.isNotEmpty() -> builder.withTokenAuth(accessToken)
        else -> builder.withGuestAuth()
    }.build()
    return server
}

fun requiredInput(name: String) = System.getenv("input_$name") ?: error("Input '$name' is not set.")
fun input(name: String) = System.getenv("input_$name") ?: ""

fun runCatchingWithLogging(block: () -> Unit) = runCatching(block).onFailure {
    fun writeMessage(text: String, vararg attributes: Pair<String, String>) =
        println(asString(MESSAGE, mapOf("text" to text, *attributes)))
    fun writeDebug(text: String) = writeMessage(text, TAGS_ATRRIBUTE to "tc:internal")
    fun writeError(text: String) = writeMessage(text, "status" to "ERROR")

    writeError("$it (Switch to 'Verbose' log level to see stacktrace)")
    writeDebug(it.stackTraceToString())
    kotlin.system.exitProcess(1)
}