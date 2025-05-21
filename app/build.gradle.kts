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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
