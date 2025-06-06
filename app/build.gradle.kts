plugins {
    id("org.jetbrains.kotlin.jvm")
}

val libs = project.extensions.getByType<org.gradle.accessors.dm.LibrariesForLibs>()

apply(plugin = "compiler.gradleplugin.plantumlgenerator")


configure<dev.benelli.gradle.PlantUmlGeneratorCompilerExtension> {
    enabled = true
    outputDirectoryPath = layout.buildDirectory.dir("generated/plantUml").get().asFile.toString()
    workflowInterfaceName = "WorkflowInterface"
    workflowMethodName = "getActivityList"
}

group = "dev.benelli"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
}

tasks.test {
    useJUnitPlatform()
}
