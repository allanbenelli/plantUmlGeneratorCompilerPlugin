import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties


plugins {
    alias(libs.plugins.kotlin.jvm)
    
    id("com.google.devtools.ksp") version "2.1.20-1.0.32"
    
    `maven-publish`
    signing

}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
//        maven("https://maven.google.com")
//        maven("https://plugins.gradle.org/m2/")
//        google()
    }
}


val rootProperties = Properties().apply {
    load(rootDir.parentFile.resolve("gradle.properties").inputStream())
}

group = "dev.benelli"
version = rootProperties["version"] as String
dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    ksp(libs.auto.service.ksp)
    implementation(libs.auto.service)
    
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])

            pom {
                name.set("compiler-plugin")
                description.set("Plant UML Generator Compiler Plugin")
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

kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    manifest.attributes["Implementation-Version"] = project.version
}


tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

