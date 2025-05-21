package dev.benelli.fir
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.coneType
import java.io.File


object UmlClassChecker : FirClassChecker(MppCheckerKind.Common) {
    private var outputDirPath: String = "build/generated/plantUml"
    private var workflowInterfaceName: String = "WorkflowInterface"
    private var workflowMethodName: String = "getActivityList"
    
    fun configure(
        outputDirPath: String,
        workflowInterfaceName: String,
        workflowMethodName: String
    ) {
        this.outputDirPath = outputDirPath
        this.workflowInterfaceName = workflowInterfaceName
        this.workflowMethodName = workflowMethodName
    }
    
    override fun check(
        declaration: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration !is FirRegularClass) return
        
        val className = declaration.name.asString()
        
        val implementsWorkflow = declaration.superTypeRefs.any {
            it.coneType.toString().contains(workflowInterfaceName)
        }
        
        if (!implementsWorkflow) return
        
        val outputDir = File(outputDirPath)
        outputDir.mkdirs()
        val output = File(outputDir, "uml-$className.puml")
        output.writeText("@startuml\nstart\n")
        
        val function = declaration.declarations
            .filterIsInstance<FirSimpleFunction>()
            .firstOrNull { it.name.asString() == workflowMethodName }
        
        if (function == null) {
            println("⚠️ Methode '$workflowMethodName' in $className nicht gefunden.")
            return
        }
        
        val body = function.body
        if (body is FirBlock) {
            val listName = findActivityListName(body)
            parseFirBody(body, output, listName)
        }
        
        output.appendText("stop\n@enduml\n")
    }
    
    private fun findActivityListName(body: FirBlock): String? {
        for (stmt in body.statements) {
            if (stmt is FirVariable) {
                val initializerCall = stmt.initializer as? FirFunctionCall
                if (initializerCall?.calleeReference?.name?.asString() == "mutableListOf") {
                    return stmt.name.asString()
                }
            }
            if (stmt is FirReturnExpression) {
                val call = stmt.result as? FirFunctionCall
                if (call?.calleeReference?.name?.asString() == "buildList") {
                    return null // Keine benannte Liste
                }
            }
        }
        return "activities" // Fallback
    }
    
    private fun parseFirBody(body: FirBlock, output: File, activityListName: String?, indent: Int = 0) {
        for (stmt in body.statements) {
            parseFirStatement(stmt, output, activityListName, indent)
        }
    }
    
    private fun parseFirStatement(
        stmt: FirStatement,
        output: File,
        activityListName: String?,
        indent: Int
    ) {
        val ind = "    ".repeat(indent)
        
        when (stmt) {
            is FirWhenExpression -> {
                if (isIfExpression(stmt)) {
                    val condition = stmt.branches.getOrNull(0)?.condition?.source?.lighterASTNode ?: "Unknown"
                    output.appendText("${ind}if ($condition?) then (yes)\n")
                    stmt.branches.getOrNull(0)?.result?.let {
                        parseFirStatement(it, output, activityListName, indent + 1)
                    }
                    stmt.branches.getOrNull(1)?.result?.let {
                        output.appendText("${ind}else (no)\n")
                        parseFirStatement(it, output, activityListName, indent + 1)
                    }
                    output.appendText("${ind}endif\n")
                } else {
                    val subject = stmt.subject?.source?.lighterASTNode ?: "Unknown"
                    output.appendText("${ind}switch ($subject)\n")
                    stmt.branches.forEach { branch ->
                        val cond = branch.condition.source?.lighterASTNode ?: "Unknown"
                        output.appendText("${ind}case (\"$cond\")\n")
                        parseFirStatement(branch.result, output, activityListName, indent + 1)
                    }
                    output.appendText("${ind}endswitch\n")
                }
            }
            
            is FirBlock -> {
                stmt.statements.forEach {
                    parseFirStatement(it, output, activityListName, indent)
                }
            }
            
            is FirFunctionCall -> {
                val callee = stmt.calleeReference.name.asString()
                val receiver = stmt.explicitReceiver?.source?.lighterASTNode?.toString()
                
                // Spezialfall: buildList-Lambda direkt behandeln
                if (callee == "buildList") {
                    val lambda = stmt.argumentList.arguments.firstOrNull() as? FirAnonymousFunction
                    val lambdaBody = lambda?.body as? FirBlock
                    if (lambdaBody != null) {
                        lambdaBody.statements.forEach { inner ->
                            if (inner is FirFunctionCall) {
                                val innerCallee = inner.calleeReference.name.asString()
                                if (innerCallee == "add" || innerCallee == "plusAssign") {
                                    val arg = inner.argumentList.arguments.firstOrNull()?.source?.lighterASTNode ?: "Unknown"
                                    output.appendText("${ind}:$arg;\n")
                                }
                            }
                        }
                    }
                    return
                }
                
                // Normale add-Aufrufe erkennen
                val isListAdd = callee == "add" || callee == "plusAssign"
                val matchesReceiver = (receiver == activityListName) || (receiver == null && activityListName == null)
                
                if (isListAdd && matchesReceiver) {
                    val arg = stmt.argumentList.arguments.firstOrNull()?.source?.lighterASTNode ?: "Unknown"
                    output.appendText("${ind}:$arg;\n")
                    return
                }
            }
            
            is FirReturnExpression -> {
                val returnExpr = stmt.result as? FirFunctionCall
                if (returnExpr?.calleeReference?.name?.asString() == "buildList") {
                    val lambda = returnExpr.argumentList.arguments.firstOrNull() as? FirAnonymousFunction
                    val lambdaBody = lambda?.body as? FirBlock
                    if (lambdaBody != null) {
                        lambdaBody.statements.forEach { inner ->
                            if (inner is FirFunctionCall) {
                                val innerCallee = inner.calleeReference.name.asString()
                                if (innerCallee == "add" || innerCallee == "plusAssign") {
                                    val arg = inner.argumentList.arguments.firstOrNull()?.source?.lighterASTNode ?: "Unknown"
                                    output.appendText("${ind}:$arg;\n")
                                }
                            }
                        }
                    }
                    return
                }
            }
            
            else -> {
                val txt = stmt.render()
                if (activityListName != null && (txt.contains("$activityListName.add") || txt.contains("$activityListName += "))) {
                    val value = txt.substringAfter("add(").substringBefore(")").ifBlank { "Unknown" }
                    output.appendText("${ind}:$value;\n")
                }
            }
        }
    }

    private fun isIfExpression(whenExpr: FirWhenExpression): Boolean {
        val branches = whenExpr.branches.size
        return when (branches) {
            1 -> (whenExpr.branches.first().condition is FirFunctionCall)
            2 -> (whenExpr.branches[1].condition is FirElseIfTrueCondition)
            else -> false
        }
    }
}
