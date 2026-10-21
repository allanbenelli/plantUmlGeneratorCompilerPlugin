import java.security.MessageDigest

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

buildscript {
    val pluginVersion: String by project

    repositories {
        //google()
        mavenCentral()
    }

    dependencies {
        classpath("dev.benelli:gradle-plugin:$pluginVersion")
    }
}
plugins {
    alias(libs.plugins.kotlin.jvm)
}





val groupId: String by project

allprojects {
    group = groupId
    description = "Kotlin compiler plugin for generating PlantUml diagrams"
    version = properties["pluginVersion"] as String
    repositories {
        mavenCentral()
//        maven("https://maven.google.com")
//        maven("https://plugins.gradle.org/m2/")
//        google()
    }
}


// Only our two plugins and their small runtime dependency closure are transferred.
// Gradle, the JDK and the consuming project's Kotlin plugin remain managed by the consumer.
val portablePluginVersion = project.version.toString()
val portableRuntime by configurations.creating

dependencies {
    portableRuntime("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
}

val stagePortableRuntime by tasks.registering {
    inputs.files(portableRuntime)
    outputs.dir(layout.buildDirectory.dir("portable-runtime-repository"))
    doLast {
        val destination = layout.buildDirectory.dir("portable-runtime-repository").get().asFile
        destination.deleteRecursively()
        fun moduleDirectory(id: org.gradle.api.artifacts.component.ModuleComponentIdentifier): File =
            destination.resolve("${id.group.replace('.', '/')}/${id.module}/${id.version}").apply { mkdirs() }
        val artifacts = portableRuntime.resolvedConfiguration.resolvedArtifacts
        artifacts.forEach { artifact ->
            val id = artifact.id.componentIdentifier as org.gradle.api.artifacts.component.ModuleComponentIdentifier
            artifact.file.copyTo(moduleDirectory(id).resolve(artifact.file.name), overwrite = true)
        }
        val poms = dependencies.createArtifactResolutionQuery()
            .forComponents(artifacts.map { it.id.componentIdentifier })
            .withArtifacts(org.gradle.maven.MavenModule::class.java, org.gradle.maven.MavenPomArtifact::class.java)
            .execute()
        check(poms.components.none { it is org.gradle.api.artifacts.result.UnresolvedComponentResult }) { "Unable to resolve portable runtime POMs" }
        poms.resolvedComponents.forEach { component ->
            val id = component.id as org.gradle.api.artifacts.component.ModuleComponentIdentifier
            component.getArtifacts(org.gradle.maven.MavenPomArtifact::class.java).forEach { result ->
                check(result is org.gradle.api.artifacts.result.ResolvedArtifactResult) { "Missing POM for $id" }
                result.file.copyTo(moduleDirectory(id).resolve("${id.module}-${id.version}.pom"), overwrite = true)
            }
        }
    }
}

val stagePortableBundle by tasks.registering(Sync::class) {
    dependsOn(
        gradle.includedBuild("compiler-plugin").task(":publishAllPublicationsToPortableRepository"),
        gradle.includedBuild("gradle-plugin").task(":publishAllPublicationsToPortableRepository"),
        stagePortableRuntime
    )
    into(layout.buildDirectory.dir("portable-bundle"))
    from(layout.buildDirectory.dir("portable-repository")) {
        include("**/${project.version}/**")
        into("repository")
    }
    from(layout.buildDirectory.dir("portable-runtime-repository")) { into("repository") }
    from("docs/offline-installation.md") { rename { "INSTALL.md" } }
    from("examples/manual-install") { into("example") }
    from("LICENSE")
}

val portableChecksums by tasks.registering {
    dependsOn(stagePortableBundle)
    // Regenerate after staging: the manifest covers every shipped file except itself.
    doLast {
        val directory = layout.buildDirectory.dir("portable-bundle").get().asFile
        directory.resolve("BUILD-INFO.txt").writeText(
            "Plugin: $portablePluginVersion\nKotlin compiler: ${libs.versions.kotlin.get()}\nJava: 25\nTested Gradle: 9.3.0\n" +
            "Runtime: org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}, org.jetbrains:annotations:13.0\n"
        )
        val manifest = directory.walkTopDown().filter { it.isFile && it.name != "SHA256SUMS" }
            .sortedBy { it.relativeTo(directory).invariantSeparatorsPath }
            .joinToString("\n", postfix = "\n") { file ->
                val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                    .joinToString("") { "%02x".format(it) }
                "$digest  ${file.relativeTo(directory).invariantSeparatorsPath}"
            }
        directory.resolve("SHA256SUMS").writeText(manifest)
    }
}

tasks.register<Zip>("portableBundle") {
    group = "distribution"
    description = "Build a transferable local Maven repository, example and SHA-256 manifest."
    dependsOn(portableChecksums)
    archiveBaseName.set("plantuml-workflow-plugin")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("portable-bundle"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
