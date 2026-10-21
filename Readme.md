# PlantUML workflow documentation

Generate activity diagrams from Kotlin workflow definitions while compiling. The
compiler plugin analyses source code; it does not execute workflows. PlantUML
renders the generated `.puml` files into images separately.

## Local installation (recommended for initial use)

Requires JDK 25. The tested toolchain is Gradle 9.3.0 and Kotlin 2.3.21.
From your local checkout:

```sh
./gradlew build :compiler-plugin:test publishToMavenLocal
```

This builds and installs both plugins into the local Maven repository (normally
`~/.m2/repository`). No Python, ZIP transfer or Maven installation is needed.
Configure `mavenLocal()` for plugin and compiler-plugin resolution in the target
project, then apply version `2.0.0` and select your interface with `workflowDocs`.

See [local installation](docs/local-installation.md) for the exact target-project
settings and build snippets. A standalone example is available:

```sh
./gradlew -p examples/local-install generateWorkflowDocumentation checkWorkflowDocumentation
```

To generate diagrams for this repository's test fixtures:

```sh
./gradlew :app:generateWorkflowDocumentation
```

The example application's output is in:

```text
app/build/generated/plantUml/compileKotlin/dev/benelli/
```

Each concrete workflow produces a `.puml` diagram and a `.diagnostics.txt` report.
The output layout is now `<configured-directory>/<compile-task>/<package>/<class>`;
update consumers of the original flat output directory accordingly. Abstract
classes and interfaces are analysed as ancestors but do not get standalone diagrams.

## Configuration

Apply `compiler.gradleplugin.plantumlgenerator` to a Kotlin JVM project and configure:

```kotlin
workflowDocs {
    enabled = true
    outputDirectory.set(layout.buildDirectory.dir("generated/plantUml"))
    workflowInterface.set("dev.benelli.WorkflowInterface")
    workflowMethod.set("getActivityList")
}
```

The fully qualified interface name is required. Use a string because your project's
classes are not available while Gradle evaluates the build script. No application
source dependency on this plugin is required. The entry method must have no value
parameters. The plugin runs for main Kotlin/JVM compilations, not tests. Configure
each module independently. The old `plantUmlGeneratorCompilerExtension` name and
`workflowInterfaceName` / `workflowMethodName` / `outputDirectoryPath` setters remain
as compatibility aliases, but now require a fully qualified interface name too.

## Optional transfer to another machine

```sh
./gradlew portableBundle
```

This builds `build/distributions/plantuml-workflow-plugin-2.0.0.zip`: a local Maven
repository with both plugin JARs, metadata, sources, their small runtime dependency
closure, a standalone example and SHA-256 checksums. The target organisation keeps
control of its JDK, Gradle and Kotlin infrastructure. Nothing is published remotely,
and the plugin does not add external repositories.

Follow [the manual installation guide](docs/offline-installation.md) for exact
settings/build-script snippets, prerequisites and offline operation.

The Gradle integration registers documentation as a compilation output. Unchanged
builds can remain up-to-date; when compilation runs, the full JVM source set is
analysed and its dedicated output subdirectory is rebuilt. This deliberately
disables Kotlin incremental compilation for enabled documentation compilations,
trading compilation speed for correct updates to inherited workflows. Do not place
handwritten files inside the compile-task output subdirectory: it is generated and
owned by the plugin. Build configuration-cache compatibility is not yet verified.

## Supported source patterns

- Direct and transitive workflow-interface inheritance.
- Inherited entry methods, overrides and interface default implementations,
  resolved through the compiler's member scope (including more specific defaults).
- No-argument member methods returning lists, including virtual hooks dispatched
  for the concrete workflow and explicit `super` calls.
- `mutableListOf`, `listOf`, `emptyList`, returned `buildList` expressions.
- `add`, scalar `+=`, and `addAll` / collection `+=` with supported list-producing calls.
- `if` / `else`, nested branches, `when` with and without a subject.
- `for` and `while`; `for` diagrams allow zero iterations.
- Selecting the directly returned local list instead of the first declared list.

Collection factories and supported list mutations are checked against resolved
Kotlin collection symbols. Recursive list-producing calls are bounded and reported.

## Analysis gaps and CI

Unknown sections are shown as red `INCOMPLETE` nodes and listed in the adjacent
report. To reject reported gaps in CI:

```sh
./gradlew :app:checkWorkflowDocumentation
```

**This command intentionally fails for the example app**, which contains fixtures
for recursion, unsupported transformations and early returns. The normal test/build
commands assert that those cases are reported and still pass.

This is a bounded structural analyser, not a full Kotlin interpreter or a proof of
workflow behaviour. In particular, arbitrary list aliases/mutations, parameterised
helper calls, external method bodies, dynamic receivers, `try`/`catch`, early returns,
and general data-flow analysis are not fully supported. Reported gaps make many
unsupported forms visible, but absence of a report is not a guarantee for arbitrary
Kotlin code or hidden side effects. Conditions are displayed, not evaluated. Source
bodies from already compiled dependencies may be unavailable.

## Architecture

- `compiler-plugin/.../fir/UmlClassChecker.kt`: workflow discovery and ancestry.
- `compiler-plugin/.../fir/FirWorkflowAnalyzer.kt`: bounded FIR-to-workflow analysis.
- `compiler-plugin/.../model/Workflow.kt`: compiler-independent model, diagnostics
  and PlantUML rendering.
- `gradle-plugin`: compiler options, output lifecycle and documentation/check tasks.
- `app`: compiler integration fixtures and expected diagrams, including inheritance
  and unsupported-case regressions.

Compiler configuration belongs to each checker instance rather than a mutable
singleton. Kotlin is pinned to 2.3.21 because FIR APIs are version-specific; the adapter rejects
a mismatched Kotlin Gradle plugin. All project modules compile to Java 25 bytecode.

Next priorities are real production regression fixtures, richer data-flow and
parameter binding, structured source locations, and compiler-version compatibility
tests. A declarative workflow DSL can be evaluated separately without forcing a
migration of existing workflows.

## Development-only integration checks

Python is used only for the optional maintainer checks below. It is not required
to build, install or use the plugin, including the portable ZIP installation.

```sh
python3 scripts/check-build-lifecycle.py
./gradlew portableBundle
python3 scripts/check-portable-install.py
```

The portable-install test uses `--offline` and needs Gradle/Kotlin prerequisites
already cached. On a development machine with approved network access, pass
`--bootstrap` to permit one dependency-resolution build before the offline test.
