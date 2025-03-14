import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

abstract class RecipeScriptIncludeTask : DefaultTask() {
    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectory
    val outputPath: Property<String> = project.objects.property(String::class.java)

    @TaskAction
    fun expandFiles() {
        inputFiles.forEach { recipeDir ->
            if (!recipeDir.isDirectory) {
                logger.error("$recipeDir is not a directory")
                return
            }
            val template = recipeDir.resolve("recipe.yml").also { it.ensureExists() }
            val expandedContent = expandYamlIncludes(template.readLines(), recipeDir)
            val resultFile = project.rootDir.resolve(outputPath.get()).resolve("${recipeDir.name}.yml")
            resultFile.writeText(expandedContent.joinToString(System.lineSeparator()))
            logger.lifecycle("Expanded includes from $template to $resultFile")
        }
    }

    private fun expandYamlIncludes(lines: List<String>, baseDir: File): List<String> {
        // This Regex matches lines of the form:
        // "<indent><key>: !include <filePath>"
        // where <indent> can be whitespace, <key> can include word characters / dashes / dots / quotes,
        // and <filePath> is everything else after "!include".
        val includePattern = Regex("""^(\s*)([\w.\-"]+)\s*:\s*!include\s+(.*)$""")

        return lines.flatMap { line ->
            // Attempt to match the pattern on each line
            includePattern.matchEntire(line)?.destructured?.let { (indent, key, filePath) ->
                // If the line matches, resolve the included file path relative to 'baseDir'
                val includedFile = File(baseDir, filePath)

                // If the file doesn't exist, insert a comment indicating the error
                val includedLines = if (includedFile.exists()) {
                    includedFile.readLines()
                } else {
                    listOf("# ERROR: Could not find included file '$filePath'")
                }

                // Replace the single line with a literal block:
                //  key: |-
                //    <content>
                listOf("$indent$key: |-") + includedLines.map { "$indent  $it" }
            } ?: listOf(line) // If no match, keep the original line
        }
    }

    private fun File.ensureExists() {
        if (!exists()) {
            throw FileNotFoundException("${this.name} file is missing for ${this.parentFile.name} recipe")
        }
    }
}
