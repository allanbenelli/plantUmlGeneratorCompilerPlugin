pluginManagement {
    repositories {
        // The transferred ZIP is unpacked one directory above this example.
        maven {
            url = uri("../repository")
            content {
                includeGroup("dev.benelli")
                includeGroup("compiler.gradleplugin.plantumlgenerator")
                includeModule("org.jetbrains.kotlin", "kotlin-stdlib")
                includeModule("org.jetbrains", "annotations")
            }
        }
        // Replace with your company's approved Kotlin-plugin repository if required.
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven { url = uri("../repository") } }
            filter { includeGroup("dev.benelli") }
        }
        maven {
            url = uri("../repository")
            content {
                includeModule("org.jetbrains.kotlin", "kotlin-stdlib")
                includeModule("org.jetbrains", "annotations")
            }
        }
        // Replace with your company's approved dependency repository if required.
        mavenCentral()
    }
}
rootProject.name = "manual-workflow-plugin-example"
