# Manual installation across trust boundaries

For a local checkout and a target project on the same machine, prefer
the simpler Maven Local workflow described in the project README. This ZIP workflow is
optional and intended for transfer to another machine.

This distribution is a **local Maven repository**, not an executable installer.
Transfer and approve the ZIP through your organisation's normal process. No GitHub
checkout, external publication or Maven server is needed at the destination.

## Compatibility

| Component | Required / tested |
| --- | --- |
| Plugin | 2.0.0 |
| Java running Gradle and the Kotlin compiler | 25 |
| Kotlin JVM plugin and compiler | **2.3.21**, exact match |
| Gradle | tested with **9.3.0** |
| Workflow source | Kotlin/JVM |

FIR compiler APIs are version-specific. Changing the workflow interface is normal
configuration; changing the Kotlin compiler version requires a compatible plugin
build and regression tests. The Gradle adapter rejects a different Kotlin plugin
version with an explicit message. Do not override Kotlin's compiler version
independently of its Gradle plugin for this distribution.

## 1. Build the transfer artifact

On the build machine with the approved JDK 25 installed:

```sh
./gradlew clean build :compiler-plugin:test portableBundle
```

Transfer `build/distributions/plantuml-workflow-plugin-2.0.0.zip`.
The ZIP contains:

- `repository/`: Gradle plugin, compiler plugin, plugin marker, source JARs and
  Maven/Gradle metadata; also the runtime dependency JARs/POMs for Kotlin stdlib
  2.3.21 and JetBrains annotations 13.0.
- `example/`: standalone consumer with its own `BusinessProcess` interface.
- `BUILD-INFO.txt`, `LICENSE`, this guide and `SHA256SUMS`.

The JDK, Gradle distribution, Kotlin Gradle plugin/compiler and application
libraries are **not bundled**. They must already be available through your
organisation's approved installations, repositories or Gradle caches. The build
machine may download these prerequisites; the destination example can be run with
`--offline` once they are available locally. The plugin itself adds no repositories
and does not execute workflows or upload source/diagrams to a service.

After extracting, verify the transferred files, for example on Linux:

```sh
sha256sum --check SHA256SUMS
```

The manifest checks file integrity; use your normal approval process for provenance.
Third-party licensing information is retained in the bundled POMs and JARs.

## 2. Point the existing build at the transferred repository

Assume the extracted directory is committed or copied to
`vendor/plantuml-workflow-plugin-2.0.0/` in the consuming project.

Merge this into **settings.gradle.kts** (do not replace your approved repositories):

```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("vendor/plantuml-workflow-plugin-2.0.0/repository")
            content {
                includeGroup("dev.benelli")
                includeGroup("compiler.gradleplugin.plantumlgenerator")
                includeModule("org.jetbrains.kotlin", "kotlin-stdlib")
                includeModule("org.jetbrains", "annotations")
            }
        }
        // Keep your company's approved Kotlin-plugin repositories here.
    }
}

dependencyResolutionManagement {
    repositories {
        // Resolve our compiler plugin exclusively from the transferred files.
        exclusiveContent {
            forRepository {
                maven { url = uri("vendor/plantuml-workflow-plugin-2.0.0/repository") }
            }
            filter { includeGroup("dev.benelli") }
        }
        maven {
            url = uri("vendor/plantuml-workflow-plugin-2.0.0/repository")
            content {
                includeModule("org.jetbrains.kotlin", "kotlin-stdlib")
                includeModule("org.jetbrains", "annotations")
            }
        }
        // Keep your company's approved application dependency repositories here.
    }
}
```

Both repository sections matter: Gradle resolves the adapter through plugin
management and resolves the compiler-plugin artifact through project dependencies.
No `mavenLocal()` or publishing into the user's home directory is required.

## 3. Select your interface in the module's build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.3.21" // Or your existing central version declaration.
    id("compiler.gradleplugin.plantumlgenerator") version "2.0.0"
}

kotlin { jvmToolchain(25) }

workflowDocs {
    workflowInterface.set("com.company.process.Workflow")
    workflowMethod.set("getActivityList") // Optional; this is the default.
    outputDirectory.set(layout.buildDirectory.dir("workflow-docs")) // Optional.
    enabled = true
}
```

The interface name is a fully qualified **string**, not `Workflow::class`. Classes
being compiled in this project are not yet available when Gradle evaluates the
build script. There is no required plugin-specific base type or annotation in your
application. Each module can configure its own interface, method and output folder.
Apply the plugin only to modules whose workflows you want documented.

The entry method must take no value parameters and describe a list of activities.
Inherited Kotlin implementations are supported when their source bodies are
available in the compilation. Already compiled dependency JARs may expose types
without analysable method bodies; these produce an analysis gap rather than a
complete reconstructed workflow. See the main README for the bounded analysis scope.

## 4. Generate and optionally validate

```sh
./gradlew :your-module:generateWorkflowDocumentation --offline
./gradlew :your-module:checkWorkflowDocumentation --offline
```

With the example configuration, output is under
`your-module/build/workflow-docs/compileKotlin/<package>/`.
Each workflow produces `.puml` plus `.diagnostics.txt`. The check task fails for
reported analysis gaps. Rendering the PlantUML text into images is a separate,
locally managed tool; the distribution does not depend on a public rendering server.

The compile-task subdirectory is generated and replaced when compilation runs.
Do not store handwritten files there. Enabled documentation compilations use a
full Kotlin compile so changes in base classes update every affected diagram.

## Standalone example

The included `example/` refers to `../repository`. Its settings use Plugin Portal
and Maven Central as illustrative sources for the existing Kotlin infrastructure;
replace those with approved repositories before connecting it in your environment,
or use `--offline` with the required tools and dependencies already cached.

From the extracted distribution, using your approved Gradle 9.3.0 executable:

```sh
gradle -p example generateWorkflowDocumentation checkWorkflowDocumentation --offline
```

The expected `OrderProcess.puml` contains `ValidateOrder()` followed by
`ShipOrder()`, derived from a base-class method and an overridden hook. You can also
use an existing approved Gradle wrapper with `-p <absolute-path-to-example>`.
