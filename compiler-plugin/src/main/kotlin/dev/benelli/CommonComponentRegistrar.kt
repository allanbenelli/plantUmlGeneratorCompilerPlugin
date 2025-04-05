package dev.benelli

import com.google.auto.service.AutoService
import dev.benelli.fir.FirPlantUmlPluginRegistrar
import dev.benelli.fir.UmlClassChecker
import dev.benelli.transform.ExampleIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import kotlin.collections.forEach

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

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        configuration.kotlinSourceRoots.forEach {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "*** Hello from ***" + it.path
            )
        }

        val logging = true
        FirExtensionRegistrarAdapter.registerExtension(FirPlantUmlPluginRegistrar())
        
        IrGenerationExtension.registerExtension(
            ExampleIrGenerationExtension(DebugLogger(logging, messageCollector))
        )
    }
}
