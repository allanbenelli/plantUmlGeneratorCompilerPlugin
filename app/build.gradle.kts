plugins {
    alias { libs.plugins.kotlin.jvm }
}

apply(plugin = "compiler.gradleplugin.plantumlgenerator")


configure<dev.benelli.gradle.PlantUmlGeneratorCompilerExtension> {
    enabled = true
    outputDirectory.set(layout.buildDirectory.dir("generated/plantUml"))
    workflowInterface.set("dev.benelli.WorkflowInterface")
    workflowMethod.set("getActivityList")
}


kotlin { jvmToolchain(25) }

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.junit.jupiter.params)
}

tasks.test {
    useJUnitPlatform()
}
