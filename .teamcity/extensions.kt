import jetbrains.buildServer.configs.kotlin.BuildStep
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.matrix

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

fun BuildStep.input(name: String, value: String) = param("env.input_${name}", value)