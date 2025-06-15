java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

buildscript {
    val pluginVersion: String by project

    repositories {
        mavenLocal()
        //google()
        mavenCentral()
    }

    dependencies {
        classpath("dev.benelli:gradle-plugin:$pluginVersion")
    }
}
plugins {
    alias(libs.plugins.kotlin.jvm)
    //kotlin("jvm") version libs.versions.kotlin apply false
}

apply(plugin = "compiler.gradleplugin.plantumlgenerator")



val groupId: String by project

allprojects {
    group = groupId
    description = "Kotlin compiler plugin for generating PlantUml diagrams"
    version = properties["pluginVersion"] as String
    repositories {
        mavenLocal()
        mavenCentral()
//        maven("https://maven.google.com")
//        maven("https://plugins.gradle.org/m2/")
//        google()
    }
}

