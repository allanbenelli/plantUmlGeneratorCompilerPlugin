rootProject.name = "plantUmlGeneratorCompilerPlugin"


includeBuild("gradle-plugin") {
    dependencySubstitution {
        substitute(module("dev.benelli:gradle-plugin:1.0.0")).using(project(":"))
    }
}
includeBuild("compiler-plugin")

include(":app")
