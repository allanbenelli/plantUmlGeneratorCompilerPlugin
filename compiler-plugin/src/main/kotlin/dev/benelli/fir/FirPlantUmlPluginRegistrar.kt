package dev.benelli.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirPlantUmlPluginRegistrar(private val configuration: WorkflowConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: org.jetbrains.kotlin.fir.FirSession -> FirPlantUmlExtension(session, configuration) }
    }
}
