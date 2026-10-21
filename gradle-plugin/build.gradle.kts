import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}
val rootProperties = Properties().apply {
    load(rootDir.parentFile.resolve("gradle.properties").inputStream())
}
group = "dev.benelli"
version = rootProperties.getProperty("pluginVersion")
repositories { mavenCentral() }
kotlin { jvmToolchain(25) }
java { withSourcesJar() }

dependencies {
    // Supplied by the consuming project's already-approved Kotlin Gradle plugin.
    compileOnly(libs.kotlin.gradle.plugin.api)
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        create("workflowDocumentation") {
            id = "compiler.gradleplugin.plantumlgenerator"
            implementationClass = "dev.benelli.gradle.PlantUmlGeneratorGradleSubPlugin"
            displayName = "PlantUML workflow documentation"
            description = "Document Kotlin workflows selected by a configurable interface."
        }
    }
}

tasks.withType<Jar>().configureEach {
    manifest.attributes["Implementation-Version"] = project.version
    manifest.attributes["Kotlin-Compiler-Version"] = libs.versions.kotlin.get()
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    repositories {
        maven {
            name = "portable"
            url = uri(rootDir.parentFile.resolve("build/portable-repository"))
        }
    }
}

tasks.processResources {
    inputs.property("kotlinVersion", libs.versions.kotlin.get())
    filesMatching("plantuml-build.properties") {
        expand("kotlinVersion" to libs.versions.kotlin.get())
    }
}
