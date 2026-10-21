# Local checkout → build → use in another project

No Python, transfer ZIP, Maven executable or running repository server is required.
Gradle publishes the built plugin artifacts into the local Maven repository
(normally `~/.m2/repository`). Both projects use the same local machine/user.

Prerequisites: JDK 25, Gradle 9.3.0 (provided by this checkout's wrapper), and Kotlin
2.3.21 in the consuming project. Build dependencies must be available from your
approved repositories or already cached.

## 1. Check out and build the plugin

```sh
git clone https://github.com/allanbenelli/plantUmlGeneratorCompilerPlugin.git
cd plantUmlGeneratorCompilerPlugin
./gradlew build :compiler-plugin:test publishToMavenLocal
```

The root `publishToMavenLocal` task publishes both the Gradle adapter and compiler
plugin, plus the plugin marker and sources. It does not publish anything remotely.
A Maven installation is not needed.

## 2. Configure repositories in the target project's settings.gradle.kts

Merge these entries with the project's existing approved repositories:

```kotlin
pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { mavenLocal() }
            filter {
                includeGroup("dev.benelli")
                includeGroup("compiler.gradleplugin.plantumlgenerator")
            }
        }
        gradlePluginPortal() // Or your approved Kotlin-plugin repository.
    }
}

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("dev.benelli") }
        }
        mavenCentral() // Or your approved application dependency repository.
    }
}
```

The two entries serve different purposes: plugin management resolves the Gradle
adapter, and dependency resolution loads the compiler plugin. Only our groups are
resolved from Maven Local; unrelated dependencies continue using existing approved
repositories. If your build manages dependency repositories in module build files,
add the second repository entry there instead of introducing a conflicting policy.

## 3. Apply and configure in the target module's build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.3.21" // Or the existing matching central declaration.
    id("compiler.gradleplugin.plantumlgenerator") version "2.0.0"
}

kotlin { jvmToolchain(25) }

workflowDocs {
    workflowInterface.set("com.company.workflow.Workflow")
    workflowMethod.set("getActivityList") // Optional; this is the default.
    outputDirectory.set(layout.buildDirectory.dir("workflow-docs")) // Optional.
}
```

Use the fully qualified name of your own interface. No plugin-specific interface
or annotation is needed in application source. This binary build requires Kotlin
2.3.21 because compiler APIs are version-specific.

## 4. Generate documentation from the target project

```sh
./gradlew :your-module:generateWorkflowDocumentation
# Optional: reject reported analysis gaps.
./gradlew :your-module:checkWorkflowDocumentation
```

Output is under `your-module/build/workflow-docs/compileKotlin/<package>/` with the
configuration above. Add `--offline` when all build prerequisites are already
cached. The local repository contains the plugin artifacts, not the JDK or the
entire Gradle/Kotlin toolchain.

## Try the separate example

After publishing, from the plugin checkout:

```sh
./gradlew -p examples/local-install generateWorkflowDocumentation checkWorkflowDocumentation
```

This is a separate Gradle build without `includeBuild` or source substitution. It
uses the published binaries and its own `BusinessProcess` interface / `steps()`
method. Expected output: `ValidateOrder()` followed by `ShipOrder()`.

## Update after local changes

Run `./gradlew build :compiler-plugin:test publishToMavenLocal` again in the plugin
checkout. Then regenerate in the target project, using `--refresh-dependencies` if
Gradle still has the previous artifact cached. When switching between multiple
local builds, use distinct `pluginVersion` values in `gradle.properties` and update
the plugin version in the consumer accordingly.

For installation on another machine across a transfer boundary, the optional
[portable ZIP workflow](offline-installation.md) remains available. For same-machine
use, Maven Local is sufficient.
