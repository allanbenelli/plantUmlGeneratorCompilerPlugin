package dev.benelli.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import kotlin.jvm.java
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.util.Properties

open class PlantUmlGeneratorCompilerExtension @javax.inject.Inject constructor(objects: org.gradle.api.model.ObjectFactory) {
    var enabled: Boolean = true
    val workflowInterface = objects.property(String::class.java)
    val workflowMethod = objects.property(String::class.java).convention("getActivityList")
    val outputDirectory = objects.directoryProperty()

    // Compatibility aliases for the original configuration block.
    var workflowInterfaceName: String
        get() = workflowInterface.orNull.orEmpty()
        set(value) { workflowInterface.set(value) }
    var workflowMethodName: String
        get() = workflowMethod.get()
        set(value) { workflowMethod.set(value) }
    var outputDirectoryPath: String
        get() = outputDirectory.orNull?.asFile?.path.orEmpty()
        set(value) { outputDirectory.fileValue(File(value)) }
}

class PlantUmlGeneratorGradleSubPlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val SERIALIZATION_GROUP_NAME = "dev.benelli"
        const val ARTIFACT_NAME = "compiler-plugin"
        val SUPPORTED_KOTLIN_VERSION: String = Properties().apply {
            PlantUmlGeneratorGradleSubPlugin::class.java.getResourceAsStream("/plantuml-build.properties")!!.use { load(it) }
        }.getProperty("kotlinVersion")
        val VERSION_NUMBER: String = PlantUmlGeneratorGradleSubPlugin::class.java.`package`.implementationVersion ?: "0.0.1"
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PlantUmlGeneratorCompilerExtension::class.java)
        val outputDirectory = extension.outputDirectory.map { directory ->
            directory.dir(kotlinCompilation.compileTaskProvider.name).asFile
        }
        kotlinCompilation.compileTaskProvider.configure { task ->
            if (extension.enabled) {
                task.outputs.dir(outputDirectory).withPropertyName("workflowDocumentation")
                // Inherited method bodies may belong to otherwise unchanged source files.
                // Reanalyse the complete compilation whenever the task runs.
                if (task is KotlinCompile) task.incremental = false
                task.doFirst {
                    val directory = outputDirectory.get()
                    // This task owns only its dedicated child directory.
                    if (directory.exists() && !directory.deleteRecursively()) {
                        error("Cannot clear workflow output directory: $directory")
                    }
                    directory.mkdirs()
                }
            }
        }
        project.tasks.named("generateWorkflowDocumentation").configure { it.dependsOn(kotlinCompilation.compileTaskProvider) }
        project.tasks.named("checkWorkflowDocumentation").configure { checkTask ->
            checkTask.dependsOn(kotlinCompilation.compileTaskProvider)
            checkTask.doLast {
                if (extension.enabled) {
                    val incomplete = outputDirectory.get().walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".diagnostics.txt") && it.readText().startsWith("INCOMPLETE") }
                        .toList()
                    if (incomplete.isNotEmpty()) {
                        throw org.gradle.api.GradleException("Incomplete workflow documentation:\n" + incomplete.joinToString("\n") { it.path })
                    }
                }
            }
        }
        return project.provider {
            val interfaceName = extension.workflowInterface.orNull.orEmpty().trim()
            if (extension.enabled) {
                require(project.getKotlinPluginVersion() == SUPPORTED_KOTLIN_VERSION) {
                    "This workflow compiler plugin requires Kotlin $SUPPORTED_KOTLIN_VERSION; found ${project.getKotlinPluginVersion()}. Use a matching plugin build."
                }
                require('.' in interfaceName && interfaceName.split('.').none { it.isBlank() }) {
                    "Configure workflowDocs.workflowInterface with a fully qualified interface name, e.g. com.acme.Workflow."
                }
                require(extension.workflowMethod.get().isNotBlank()) { "workflowDocs.workflowMethod must not be blank." }
            }
            listOf(
                SubpluginOption("enabled", extension.enabled.toString()),
                SubpluginOption("outputDirectoryPath", outputDirectory.get().absolutePath),
                SubpluginOption("workflowInterfaceName", interfaceName),
                SubpluginOption("workflowMethodName", extension.workflowMethodName)
            )
        }
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create(
            "workflowDocs", PlantUmlGeneratorCompilerExtension::class.java
        )
        extension.outputDirectory.convention(target.layout.buildDirectory.dir("generated/plantUml"))
        target.extensions.add("plantUmlGeneratorCompilerExtension", extension)
        target.tasks.register("generateWorkflowDocumentation") {
            it.group = "documentation"
            it.description = "Compile main sources and generate workflow diagrams and analysis reports."
        }
        target.tasks.register("checkWorkflowDocumentation") {
            it.group = "verification"
            it.description = "Fail if any generated workflow contains a reported analysis gap."
        }
        super.apply(target)
    }

    override fun getCompilerPluginId(): String = "plantUmlGeneratorCompilerPlugin"

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.name == "main" &&
            kotlinCompilation.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = SERIALIZATION_GROUP_NAME,
        artifactId = ARTIFACT_NAME,
        version = VERSION_NUMBER // remember to bump this version before any release!
    )

}
