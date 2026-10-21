@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class, org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm::class)

package dev.benelli.fir

import dev.benelli.model.PlantUmlRenderer
import dev.benelli.model.Workflow
import dev.benelli.model.WorkflowNode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctionsWithBaseScope
import org.jetbrains.kotlin.fir.scopes.impl.filterOutOverriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import java.io.File

data class WorkflowConfiguration(
    val outputDirectory: String,
    val interfaceName: String,
    val methodName: String
)

class UmlClassChecker(private val configuration: WorkflowConfiguration) : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass || declaration.classKind == ClassKind.INTERFACE ||
            declaration.status.modality == Modality.ABSTRACT) return
        val hierarchy = hierarchy(declaration, context.session)
        if (hierarchy.none {
            val id = it.symbol.classId
            if ('.' in configuration.interfaceName) id.asSingleFqName().asString() == configuration.interfaceName
            else id.shortClassName.asString() == configuration.interfaceName
        }) return

        val scope = declaration.unsubstitutedScope()
        fun resolveMethod(name: Name): FirNamedFunction? {
            val candidates = mutableListOf<FirNamedFunction>()
            scope.processFunctionsByName(name) { symbol ->
                val method = symbol.fir
                if (method.valueParameters.isEmpty() && method.receiverParameter == null) {
                    if (symbol.isIntersectionOverride) {
                        val inherited = filterOutOverriddenFunctions(scope.getDirectOverriddenFunctionsWithBaseScope(symbol))
                        candidates += inherited.map { it.member.fir.unwrapFakeOverrides() }
                    } else {
                        candidates += method.unwrapFakeOverrides()
                    }
                }
            }
            return candidates.distinct().singleOrNull()
        }
        val function = resolveMethod(Name.identifier(configuration.methodName))
        val analyzer = FirWorkflowAnalyzer(hierarchy, ::resolveMethod)
        val nodes = function?.let { analyzer.analyze(it) }
            ?: listOf(WorkflowNode.Unknown("No source body for ${configuration.methodName}"))
        val outputDir = File(configuration.outputDirectory).apply { mkdirs() }
        val relativePath = declaration.symbol.classId.asSingleFqName().asString().replace('.', '/')
        val output = File(outputDir, "$relativePath.puml")
        output.parentFile.mkdirs()
        val workflow = Workflow(nodes)
        output.writeText(PlantUmlRenderer.render(workflow))
        File(outputDir, "$relativePath.diagnostics.txt").writeText(
            if (workflow.diagnostics.isEmpty()) "No analysis gaps detected within the supported subset.\n"
            else workflow.diagnostics.joinToString("\n", prefix = "INCOMPLETE\n", postfix = "\n")
        )
    }

    private fun hierarchy(root: FirRegularClass, session: FirSession): List<FirRegularClass> {
        val result = mutableListOf<FirRegularClass>()
        val seen = mutableSetOf<org.jetbrains.kotlin.name.ClassId>()
        fun visit(declaration: FirRegularClass) {
            if (!seen.add(declaration.symbol.classId)) return
            result.add(declaration)
            declaration.superTypeRefs.mapNotNull { it.coneType.toRegularClassSymbol(session)?.fir }.sortedBy { it.classKind == ClassKind.INTERFACE }.forEach(::visit)
        }
        visit(root)
        return result
    }
}
