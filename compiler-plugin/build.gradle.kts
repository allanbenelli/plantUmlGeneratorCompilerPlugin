import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
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
    compileOnly(libs.kotlin.compiler.embeddable)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions.freeCompilerArgs.addAll("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi", "-Xcontext-parameters")
}
tasks.test { useJUnitPlatform() }
tasks.withType<Jar>().configureEach {
    manifest.attributes["Implementation-Version"] = project.version
    manifest.attributes["Kotlin-Compiler-Version"] = libs.versions.kotlin.get()
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("compilerPlugin") {
            from(components["java"])
            pom {
                name.set("PlantUML workflow compiler plugin")
                description.set("Generate workflow documentation during Kotlin compilation.")
                url.set("https://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin")
                licenses { license { name.set("Apache License 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
            }
        }
    }
    repositories {
        maven {
            name = "portable"
            url = uri(rootDir.parentFile.resolve("build/portable-repository"))
        }
    }
}
