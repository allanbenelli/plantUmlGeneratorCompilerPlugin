plugins {
    kotlin("jvm") version "2.3.21"
    id("compiler.gradleplugin.plantumlgenerator") version "2.0.0"
}

kotlin { jvmToolchain(25) }

workflowDocs {
    workflowInterface.set("com.acme.process.BusinessProcess")
    workflowMethod.set("steps")
    outputDirectory.set(layout.buildDirectory.dir("workflow-docs"))
}
