import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

class RecipesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "recipes", RecipesPluginExtension::class.java, project.objects
        )

        project.tasks.register<RecipeScriptIncludeTask>("recipes") {
            inputFiles.from(extension.inputFiles)
            outputPath.set(extension.outputPath)
        }
    }
}

open class RecipesPluginExtension @Inject constructor(objects: ObjectFactory) {
    val inputFiles: ConfigurableFileCollection = objects.fileCollection()
    val outputPath: Property<String> = objects.property(String::class.java)

    fun add(path: String) {
        inputFiles.from(path)
    }

    fun setOutputPath(path: String) {
        outputPath.set(path)
    }
}
