package dev.benelli.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import kotlin.jvm.java

open class PlantUmlGeneratorCompilerExtension {
    var enabled: Boolean = true
    var outputDirectoryPath: String = ""
    var workflowInterfaceName: String = "WorkflowInterface"
    var workflowMethodName: String = "getActivityList"
}

class PlantUmlGeneratorGradleSubPlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val SERIALIZATION_GROUP_NAME = "dev.benelli"
        const val ARTIFACT_NAME = "compiler-plugin"
        val VERSION_NUMBER: String = PlantUmlGeneratorGradleSubPlugin::class.java.`package`.implementationVersion ?: "0.0.1"
    }

    private var gradleExtension : PlantUmlGeneratorCompilerExtension = PlantUmlGeneratorCompilerExtension()
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        gradleExtension = kotlinCompilation.target.project.extensions.findByType(PlantUmlGeneratorCompilerExtension::class.java) ?: PlantUmlGeneratorCompilerExtension()
        
        return kotlinCompilation.target.project.provider {
            listOf(
                SubpluginOption("enabled", gradleExtension.enabled.toString()),
                SubpluginOption("outputDirectoryPath", gradleExtension.outputDirectoryPath),
                SubpluginOption("workflowInterfaceName", gradleExtension.workflowInterfaceName),
                SubpluginOption("workflowMethodName", gradleExtension.workflowMethodName)
            )
        }
        
    }

    override fun apply(target: Project) {
        target.extensions.create(
            "plantUmlGeneratorCompilerExtension",
            PlantUmlGeneratorCompilerExtension::class.java
        )
        super.apply(target)
    }

    override fun getCompilerPluginId(): String = "plantUmlGeneratorCompilerPlugin"

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return true
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = SERIALIZATION_GROUP_NAME,
        artifactId = ARTIFACT_NAME,
        version = VERSION_NUMBER // remember to bump this version before any release!
    )

}
