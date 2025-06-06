import org.gradle.api.publish.PublishingExtension

val javaVersion = providers.gradleProperty("javaVersion").orNull?.toInt() ?: 17

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion.toString()))
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories { mavenLocal() }
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    dependsOn(gradle.includedBuild("compiler-plugin").task("publishToMavenLocal"))
    dependsOn(gradle.includedBuild("gradle-plugin").task("publishToMavenLocal"))
}

buildscript {

    repositories {
        mavenLocal()
        //google()
        mavenCentral()
    }

    dependencies {
        classpath("dev.benelli:gradle-plugin:1.0.0")
    }
}
plugins {
    alias(libs.plugins.kotlin.jvm)
    //kotlin("jvm") version libs.versions.kotlin apply false
}

apply(plugin = "compiler.gradleplugin.plantumlgenerator")

System.setProperty("kotlin.compiler.execution.strategy", "in-process") // For debugging



allprojects {
    group = "dev.benelli"
    description = "Kotlin compiler plugin for generating PlantUml diagrams"
    version = properties["version"] as String
    repositories {
        mavenLocal()
        mavenCentral()
//        maven("https://maven.google.com")
//        maven("https://plugins.gradle.org/m2/")
//        google()
    }
}

