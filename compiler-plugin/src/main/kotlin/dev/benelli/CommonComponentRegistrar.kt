package dev.benelli

import com.google.auto.service.AutoService
import dev.benelli.fir.FirPlantUmlPluginRegistrar
import dev.benelli.fir.UmlClassChecker
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
class CommonComponentRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }
        
        val outputPath = configuration.get(KEY_OUTPUT_DIR) ?: "build"
        val workflowInterface = configuration.get(KEY_WORKFLOW_INTERFACE) ?: "WorkflowInterface"
        val workflowMethod = configuration.get(KEY_WORKFLOW_METHOD) ?: "getActivityList"
        
        UmlClassChecker.configure(
            outputDirPath = outputPath,
            workflowInterfaceName = workflowInterface,
            workflowMethodName = workflowMethod
        )

        FirExtensionRegistrarAdapter.registerExtension(FirPlantUmlPluginRegistrar())
    }
}
