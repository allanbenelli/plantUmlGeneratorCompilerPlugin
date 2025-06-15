rootProject.name = "plantUmlGeneratorCompilerPlugin"

val pluginVersion: String by settings

includeBuild("gradle-plugin") {
    dependencySubstitution {
        substitute(module("dev.benelli:gradle-plugin:$pluginVersion")).using(project(":"))
    }
}
includeBuild("compiler-plugin")
include(":app")

