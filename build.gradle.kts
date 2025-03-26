import org.gradle.kotlin.dsl.libs

buildscript {

    repositories {
        mavenLocal()
        //google()
        mavenCentral()
    }

    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.25.3")
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

