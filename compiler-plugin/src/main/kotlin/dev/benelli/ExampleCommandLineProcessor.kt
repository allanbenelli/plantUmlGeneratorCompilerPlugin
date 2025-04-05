package dev.benelli

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import kotlin.text.toBoolean

@AutoService(CommandLineProcessor::class) // don't forget!
class ExampleCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "plantUmlGeneratorCompilerPlugin"
    
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = "enabled", valueDescription = "<true|false>",
            description = "whether to enable the plugin or not"
        ),
        CliOption(
            optionName = "outputDirectoryPath", valueDescription = "<path>",
            description = "output path for generated PlantUML files", required = false
        ),
        CliOption(
            optionName = "workflowInterfaceName",
            valueDescription = "<interfaceName>",
            description = "fully qualified name of the workflow interface",
            required = false
        ),
        CliOption(
            optionName = "workflowMethodName",
            valueDescription = "<methodName>",
            description = "name of the workflow method to analyze",
            required = false
        )
    
    )
    
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
            "outputDirectoryPath" -> configuration.put(KEY_OUTPUT_DIR, value)
            "workflowInterfaceName" -> configuration.put(KEY_WORKFLOW_INTERFACE, value)
            "workflowMethodName" -> configuration.put(KEY_WORKFLOW_METHOD, value)
        }
    }
}

val KEY_ENABLED = CompilerConfigurationKey<Boolean>("whether the plugin is enabled")
val KEY_OUTPUT_DIR = CompilerConfigurationKey<String>("output directory path")
val KEY_WORKFLOW_INTERFACE = CompilerConfigurationKey<String>("workflow interface name")
val KEY_WORKFLOW_METHOD = CompilerConfigurationKey<String>("workflow method name")
