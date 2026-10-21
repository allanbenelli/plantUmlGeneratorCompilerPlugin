package dev.benelli.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowTest {
    @Test
    fun nestedAnalysisGapsAreReportedAndRendered() {
        val workflow = Workflow(listOf(WorkflowNode.Decision("enabled", listOf(
            WorkflowNode.Loop("ready", listOf(WorkflowNode.Unknown("external producer")))
        ), emptyList())))
        assertEquals(listOf("external producer"), workflow.diagnostics)
        val diagram = PlantUmlRenderer.render(workflow)
        assertTrue(diagram.contains("INCOMPLETE - external producer"))
        assertTrue(diagram.contains("else (no)\nendif"))
        assertTrue(diagram.endsWith("stop\n@enduml\n"))
    }
}
