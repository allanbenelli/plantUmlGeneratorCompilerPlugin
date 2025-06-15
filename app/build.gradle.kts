plugins {
    alias { libs.plugins.kotlin.jvm }
}

apply(plugin = "compiler.gradleplugin.plantumlgenerator")


configure<dev.benelli.gradle.PlantUmlGeneratorCompilerExtension> {
    enabled = true
    outputDirectoryPath = layout.buildDirectory.dir("generated/plantUml").get().asFile.toString()
    workflowInterfaceName = "WorkflowInterface"
    workflowMethodName = "getActivityList"
}


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
