
<h1>PlantUmlGeneratorCompilerPlugin </h1>

## Usage

* Inside the project folder run `./gradlew clean build`
* Publish the compiler and Gradle plugin to your local Maven repository with `./gradlew publishToMavenLocal`

The plugin is only active when the build cache is changed. This is why you need to run "clean" before building, when you want to see the log output again.

### Project Structure
*  <kbd>app</kbd> - A Kotlin project which applies a gradle plugin(compiler.gradleplugin.plantumlgenerator) which triggers the compiler plugin.
*  <kbd>compiler-plugin</kbd> - This module contains the Kotlin Compiler Plugin
*  <kbd>gradle-plugin</kbd> - This module contains the gradle plugin which trigger the compiler plugin
