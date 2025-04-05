plugins {
    kotlin("jvm") version("2.1.20")
    id("java-gradle-plugin")
    `maven-publish`
}

group = "dev.benelli"
version = "1.0.0"


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
//        maven("https://maven.google.com")
//        maven("https://plugins.gradle.org/m2/")
//        google()
    }
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.20")
}

gradlePlugin {
    plugins {

        create("simplePlugin") {
            id = "compiler.gradleplugin.plantumlgenerator"
            implementationClass = "dev.benelli.gradle.PlantUmlGeneratorGradleSubPlugin"
        }
    }
}

tasks.register("sourcesJar", Jar::class) {
    group = "build"
    description = "Assembles Kotlin sources"

    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn(tasks.classes)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            //artifact(tasks["dokkaJar"])

            pom {
                name.set("compiler.gradleplugin.plantumlgenerator")
                description.set("KotlinCompilerPluginExample")
                url.set("https://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin/blob/master/LICENSE.txt")
                    }
                }
                scm {
                    url.set("https://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin")
                    connection.set("scm:git:git://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin.git")
                }
                developers {
                    developer {
                        name.set("Allan Benelli")
                        url.set("https://github.com/allanbenelli")
                    }
                }
            }
        }
    }

    repositories {
        if (
            hasProperty("sonatypeUsername") &&
            hasProperty("sonatypePassword") &&
            hasProperty("sonatypeSnapshotUrl") &&
            hasProperty("sonatypeReleaseUrl")
        ) {
            maven {
                val url = when {
                    "SNAPSHOT" in version.toString() -> property("sonatypeSnapshotUrl")
                    else -> property("sonatypeReleaseUrl")
                } as String
                setUrl(url)
                credentials {
                    username = property("sonatypeUsername") as String
                    password = property("sonatypePassword") as String
                }
            }
        }
    }
}


tasks.build {
    dependsOn(":compiler-plugin:publishToMavenLocal")

}