package dev.benelli

import dev.benelli.fir.FirPlantUmlPluginRegistrar
import dev.benelli.fir.WorkflowConfiguration
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class CommonComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "plantUmlGeneratorCompilerPlugin"

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }
        
        val outputPath = configuration.get(KEY_OUTPUT_DIR) ?: "build"
        val workflowInterface = configuration.get(KEY_WORKFLOW_INTERFACE) ?: "WorkflowInterface"
        val workflowMethod = configuration.get(KEY_WORKFLOW_METHOD) ?: "getActivityList"
        
        FirExtensionRegistrarAdapter.registerExtension(
            FirPlantUmlPluginRegistrar(WorkflowConfiguration(outputPath, workflowInterface, workflowMethod))
        )
    }
}
