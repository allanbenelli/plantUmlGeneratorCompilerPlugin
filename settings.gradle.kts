rootProject.name = "plantUmlGeneratorCompilerPlugin"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("gradle/libs.versions.toml")) }
    }
}


includeBuild("gradle-plugin") {
    dependencySubstitution {
        substitute(module("dev.benelli:gradle-plugin:1.0.0")).using(project(":"))
    }
}
includeBuild("compiler-plugin")
include(":app")

