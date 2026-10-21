pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { mavenLocal() }
            filter {
                includeGroup("dev.benelli")
                includeGroup("compiler.gradleplugin.plantumlgenerator")
            }
        }
        // Replace with your approved Kotlin-plugin repository if required.
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("dev.benelli") }
        }
        // Replace with your approved application dependency repository if required.
        mavenCentral()
    }
}
rootProject.name = "local-workflow-plugin-example"
