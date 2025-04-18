import jetbrains.buildServer.configs.kotlin.BuildStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.triggers.vcs

fun BuildType.enableMatrixBuild() {
    features {
        matrix {
            os = listOf(
                value("Linux"),
                value("Windows"),
                value("Mac OS")
            )
        }
    }
}

fun BuildType.enableVcsTrigger() {
    triggers {
        vcs {
            branchFilter =  """
            +:refs/heads/main
            +:main
            +:<default>
        """.trimIndent()
        }
    }
}

fun BuildStep.input(name: String, value: String) = param("env.input_${name}", value)