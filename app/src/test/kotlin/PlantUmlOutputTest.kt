import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

class PlantUmlOutputTest {
    
    @Test
    fun unrelatedInterfaceDoesNotGenerateWorkflow() {
        assertFalse(actualDir.resolve("UnrelatedWorkflow.puml").toFile().exists())
    }

    @Test
    fun unsupportedAndRecursiveCallsRemainVisible() {
        for (name in listOf("RecursiveWorkflow", "UnsupportedWorkflow", "EarlyReturnWorkflow")) {
            val diagram = actualDir.resolve("$name.puml").toFile().readText()
            assertTrue(diagram.contains("INCOMPLETE"), diagram)
            assertTrue(diagram.trim().endsWith("@enduml"), diagram)
        }
    }

    @Test
    fun abstractWorkflowsDoNotGenerateMisleadingDiagrams() {
        assertFalse(actualDir.resolve("TemplateWorkflow.puml").toFile().exists())
        assertFalse(actualDir.resolve("DefaultWorkflow.puml").toFile().exists())
    }

    companion object {
        private val expectedDir = Paths.get("src/test/resources/expectedOutput")
        private val actualDir = Paths.get("build/generated/plantUml/compileKotlin/dev/benelli")
        
        @JvmStatic
        fun expectedFilesProvider(): Stream<File> {
            return Files.walk(expectedDir)
                .filter { Files.isRegularFile(it) }
                .map { it.toFile() }
        }
    }
    
    @ParameterizedTest(name = "Vergleich von {0}")
    @MethodSource("expectedFilesProvider")
    fun testGeneratedOutputMatchesExpected(expectedFile: File) {
        val relativePath = expectedDir.relativize(expectedFile.toPath())
        val actualFile = actualDir.resolve(relativePath).toFile()
        
        assertTrue(actualFile.exists()) { "Erwartete Datei nicht gefunden: ${actualFile.path}" }
        
        val expectedContent = expectedFile.readText().trim()
        val actualContent = actualFile.readText().trim()
        
        assertEquals(expectedContent, actualContent, "Inhalt der Datei ${relativePath} stimmt nicht überein.")
        val report = actualDir.resolve(relativePath.toString().removeSuffix(".puml") + ".diagnostics.txt").toFile()
        assertFalse(report.readText().contains("INCOMPLETE"), report.readText())
    }
}
