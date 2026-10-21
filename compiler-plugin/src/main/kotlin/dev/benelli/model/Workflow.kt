package dev.benelli.model

/** Compiler-independent representation; unsupported sections remain visible. */
sealed interface WorkflowNode {
    data class Step(val label: String) : WorkflowNode
    data class Decision(val condition: String, val yes: List<WorkflowNode>, val no: List<WorkflowNode>?) : WorkflowNode
    data class Selection(val subject: String, val cases: List<Pair<String, List<WorkflowNode>>>) : WorkflowNode
    data class Loop(val condition: String, val body: List<WorkflowNode>) : WorkflowNode
    data class ForEach(val variable: String, val range: String, val body: List<WorkflowNode>) : WorkflowNode
    data class Unknown(val reason: String) : WorkflowNode
}

data class Workflow(val nodes: List<WorkflowNode>) {
    val diagnostics: List<String> get() = nodes.flatMap { it.diagnostics() }
}

private fun WorkflowNode.diagnostics(): List<String> = when (this) {
    is WorkflowNode.Unknown -> listOf(reason)
    is WorkflowNode.Decision -> (yes + no.orEmpty()).flatMap { it.diagnostics() }
    is WorkflowNode.Selection -> cases.flatMap { it.second }.flatMap { it.diagnostics() }
    is WorkflowNode.Loop -> body.flatMap { it.diagnostics() }
    is WorkflowNode.ForEach -> body.flatMap { it.diagnostics() }
    is WorkflowNode.Step -> emptyList()
}

object PlantUmlRenderer {
    fun render(workflow: Workflow): String = buildString {
        append("@startuml\nstart\n")
        appendNodes(workflow.nodes, 0)
        append("stop\n@enduml\n")
    }

    private fun StringBuilder.appendNodes(nodes: List<WorkflowNode>, depth: Int) {
        val indent = "    ".repeat(depth)
        fun line(text: String) { append(indent).append(text).append('\n') }
        for (node in nodes) when (node) {
            is WorkflowNode.Step -> line(":${node.label};")
            is WorkflowNode.Unknown -> {
                line("#MistyRose:INCOMPLETE - ${node.reason.replace('\n', ' ').replace(';', ',')};")
            }
            is WorkflowNode.Decision -> {
                line("if (${node.condition}?) then (yes)")
                appendNodes(node.yes, depth + 1)
                node.no?.let { line("else (no)"); appendNodes(it, depth + 1) }
                line("endif")
            }
            is WorkflowNode.Selection -> {
                line("switch (${node.subject})")
                node.cases.forEach { (condition, body) ->
                    line("case (\"$condition\")")
                    appendNodes(body, depth + 1)
                }
                line("endswitch")
            }
            is WorkflowNode.Loop -> {
                line("while (${node.condition}) is (true)")
                appendNodes(node.body, depth + 1)
                line("endwhile")
            }
            is WorkflowNode.ForEach -> {
                line("while (for ${node.variable} in (${node.range})) is (next)")
                appendNodes(node.body, depth + 1)
                line("endwhile (done)")
            }
        }
    }
}
