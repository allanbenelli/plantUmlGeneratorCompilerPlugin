package dev.benelli.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirPlantUmlPluginRegistrar : FirExtensionRegistrar() {
    init {
        println(">>> 🧪 FirPluginRegistrar wird instanziert!")
    }
    
    override fun ExtensionRegistrarContext.configurePlugin() {
        println(">>> 🚀 FIR Plugin aktiv (Handler-basierte Registrierung)")
        +::FirPlantUmlExtension
    }
}
