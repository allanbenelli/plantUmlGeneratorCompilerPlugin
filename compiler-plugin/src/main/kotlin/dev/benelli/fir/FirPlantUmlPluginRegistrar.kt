package dev.benelli.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirPlantUmlPluginRegistrar : FirExtensionRegistrar() {
    
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirPlantUmlExtension
    }
}
