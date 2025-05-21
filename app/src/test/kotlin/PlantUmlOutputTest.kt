import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

class PlantUmlOutputTest {
    
    companion object {
        private val expectedDir = Paths.get("src/test/resources/expectedOutput")
        private val actualDir = Paths.get("build/generated/plantUml")
        
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
        
        assert(actualFile.exists()) { "Erwartete Datei nicht gefunden: ${actualFile.path}" }
        
        val expectedContent = expectedFile.readText().trim()
        val actualContent = actualFile.readText().trim()
        
        assertEquals(expectedContent, actualContent, "Inhalt der Datei ${relativePath} stimmt nicht überein.")
    }
}
