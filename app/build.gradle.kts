plugins {
    id("org.jetbrains.kotlin.jvm")
}

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
}

tasks.test {
    useJUnitPlatform()
}
