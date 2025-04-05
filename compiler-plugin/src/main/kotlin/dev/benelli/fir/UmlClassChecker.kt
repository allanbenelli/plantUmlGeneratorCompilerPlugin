package dev.benelli.fir
import dev.benelli.KEY_OUTPUT_DIR
import dev.benelli.KEY_WORKFLOW_INTERFACE
import dev.benelli.KEY_WORKFLOW_METHOD
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
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
        
        if (!implementsWorkflow)  {
            return
        }
        
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
            parseFirBody(body, output, activityListName = "activities")
        }
        
        output.appendText("stop\n@enduml\n")
    }
    
    private fun parseFirBody(
        body: FirBlock,
        output: File,
        activityListName: String,
        indent: Int = 0
    ) {
        for (statement in body.statements) {
            parseFirStatement(statement, output, activityListName, indent)
        }
    }
    
    private fun parseFirStatement(
        stmt: FirStatement,
        output: File,
        activityListName: String,
        indent: Int
    ) {
        val ind = "    ".repeat(indent)
        
        when (stmt) {
            is FirWhenExpression -> {
                if (isIfExpression(stmt)) {
                    val condition = stmt.branches.getOrNull(0)?.condition?.source?.lighterASTNode ?: "Unknown Condition"
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
                        val cond = branch.condition.source?.lighterASTNode
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
                val receiver = stmt.explicitReceiver?.source?.lighterASTNode.toString()
                val callee = stmt.calleeReference.name.asString()
                
                if (callee == "buildList") {
                    val lambda = stmt.argumentList.arguments.firstOrNull() as? FirAnonymousFunction
                    val lambdaBody = lambda?.body as? FirBlock
                    if (lambdaBody != null) {
                        parseFirBody(lambdaBody, output, activityListName, indent)
                    }
                    return
                }
                
                if ((receiver == activityListName && callee == "add") || callee == "plusAssign") {
                    val arg = stmt.argumentList.arguments.firstOrNull()?.source?.lighterASTNode ?: "Unknown"
                    output.appendText("${ind}:$arg;\n")
                }
                
                
//                if ((receiver == activityListName && callee == "add") || callee == "plusAssign") {
//                    stmt.argumentList.arguments.forEach {
//                        parseFirStatement(it, output, activityListName, indent)
//                    }
//                } else if (callee.contains("Activity") && !callee.contains("Context")){
//                    output.appendText("${ind}:$callee;\n")
//                }
                
            }
            
            is FirReturnExpression -> {
                val returnExpr = stmt.result as? FirFunctionCall
                if (returnExpr?.calleeReference?.name?.asString() == "buildList") {
                    val lambda = returnExpr.argumentList.arguments.firstOrNull() as? FirAnonymousFunction
                    val lambdaBody = lambda?.body as? FirBlock
                    if (lambdaBody != null) {
                        parseFirBody(lambdaBody, output, activityListName, indent)
                    }
                }
            }
            
            else -> {
                val txt = stmt.render()
                if (txt.contains("$activityListName.add") || txt.contains("$activityListName += ")) {
                    val value = txt.substringAfter("add(").substringBefore(")").ifBlank { "Unknown" }
                    output.appendText("${ind}:$value;\n")
                }
            }
        }
    }
    
    private fun isIfExpression(whenExpr: FirWhenExpression): Boolean {
        val branches = whenExpr.branches.size
        
        //return whenExpr.branches.size == 2 && whenExpr.branches[1].condition is FirElseIfTrueCondition
        
        return when (branches) {
            1 -> (whenExpr.branches.first().condition is FirFunctionCall) == true
            2 -> (whenExpr.branches[1].condition is FirElseIfTrueCondition) == true
            else -> false
        }
    }
}
