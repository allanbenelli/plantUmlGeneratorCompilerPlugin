@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package dev.benelli.fir

import dev.benelli.model.WorkflowNode
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.classId

/** A deliberately bounded source analysis. Unresolved list producers are visible in the diagram. */
internal class FirWorkflowAnalyzer(
    private val hierarchy: List<FirRegularClass>,
    private val resolveMethod: (Name) -> FirNamedFunction?
) {
    private val activeFunctions = mutableSetOf<FirNamedFunction>()

    fun analyze(function: FirNamedFunction): List<WorkflowNode> {
        if (!activeFunctions.add(function)) return unknown("Recursive call to ${function.name}")
        return try {
            val body = function.body ?: return unknown("No source body for ${function.name}")
            val gaps = mutableListOf<WorkflowNode>()
            body.accept(object : org.jetbrains.kotlin.fir.visitors.FirVisitorVoid() {
                override fun visitElement(element: org.jetbrains.kotlin.fir.FirElement) { element.acceptChildren(this) }
                override fun visitReturnExpression(returnExpression: FirReturnExpression) {
                    if (returnExpression.target.labeledElement === function && returnExpression !== body.statements.lastOrNull()) {
                        gaps += WorkflowNode.Unknown("Early return requires control-flow analysis: ${text(returnExpression)}")
                    }
                    returnExpression.acceptChildren(this)
                }
            })
            gaps + parse(body, findList(body), mutableMapOf())
        } finally { activeFunctions.remove(function) }
    }

    private fun findList(body: FirBlock): String? {
        // Prefer the returned variable over whichever mutable list happens to be declared first.
        val returned = body.statements.filterIsInstance<FirReturnExpression>().lastOrNull()?.result
        if (returned is FirPropertyAccessExpression) return (returned.calleeReference as? org.jetbrains.kotlin.fir.references.FirNamedReference)?.name?.asString()
        return body.statements.filterIsInstance<FirVariable>().firstOrNull {
            (it.initializer as? FirFunctionCall)?.calleeReference?.name?.asString() == "mutableListOf"
        }?.name?.asString()
    }

    private fun text(expression: FirStatement?): String =
        expression?.source?.lighterASTNode?.toString() ?: expression?.render() ?: "Unknown"

    private fun unknown(reason: String) = listOf(WorkflowNode.Unknown(reason))

    private fun parse(stmt: FirStatement, list: String?, vars: MutableMap<String, String>): List<WorkflowNode> = when (stmt) {
        is FirBlock -> parseBlock(stmt, list, vars)
        is FirVariable -> {
            vars[stmt.name.asString()] = text(stmt.initializer)
            if (stmt.name.asString() == list) {
                val initializer = stmt.initializer as? FirFunctionCall
                if (initializer != null) produceList(initializer, vars) else unknown("Unsupported list initializer: ${stmt.name}")
            } else emptyList()
        }
        is FirWhenExpression -> {
            if (stmt.source?.elementType == org.jetbrains.kotlin.KtNodeTypes.IF) {
                listOf(WorkflowNode.Decision(
                    text(stmt.branches.firstOrNull()?.condition),
                    stmt.branches.firstOrNull()?.result?.let { parse(it, list, vars.toMutableMap()) } ?: emptyList(),
                    stmt.branches.getOrNull(1)?.result?.let { parse(it, list, vars.toMutableMap()) }
                ))
            } else {
                val subject = stmt.subjectVariable?.initializer?.let(::text) ?: "true"
                listOf(WorkflowNode.Selection(vars[subject] ?: subject, stmt.branches.map {
                    (if (stmt.subjectVariable == null && it.condition !is FirElseIfTrueCondition) text(it.condition) else condition(it.condition)) to parse(it.result, list, vars.toMutableMap())
                }))
            }
        }
        is FirWhileLoop -> listOf(WorkflowNode.Loop(text(stmt.condition), parse(stmt.block, list, vars.toMutableMap())))
        is FirFunctionCall -> {
            val name = stmt.calleeReference.name.asString()
            val receiver = stmt.explicitReceiver?.let(::text)
            val matches = receiver == list || (list == null && (receiver == "this" || receiver == null))
            val symbol = (stmt.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
            val collectionOperation = symbol?.callableId?.packageName?.asString() == "kotlin.collections"
            val collectionArgument = name == "plusAssign" && symbol?.fir?.valueParameters?.firstOrNull()
                ?.returnTypeRef?.coneType?.classId?.asSingleFqName()?.asString() in
                setOf("kotlin.collections.Iterable", "kotlin.Array", "kotlin.sequences.Sequence")
            when {
                name == "buildList" -> produceList(stmt, vars)
                matches && collectionOperation && (name == "add" || (name == "plusAssign" && !collectionArgument)) -> {
                    val arg = stmt.argumentList.arguments.firstOrNull()
                    listOf(WorkflowNode.Step(text(arg)))
                }
                matches && collectionOperation && (name == "addAll" || collectionArgument) -> {
                    val argument = stmt.argumentList.arguments.firstOrNull()
                    if (argument is FirFunctionCall) produceList(argument, vars)
                    else unknown("Unsupported addAll argument: ${text(argument)}")
                }
                // These calls update loop counters; their conditions are already represented.
                name in setOf("inc", "dec") -> emptyList()
                else -> unknown("Unanalysed call: ${text(stmt)}")
            }
        }
        is FirReturnExpression -> when (val result = stmt.result) {
            is FirFunctionCall -> produceList(result, vars)
            is FirPropertyAccessExpression -> if ((result.calleeReference as? org.jetbrains.kotlin.fir.references.FirNamedReference)?.name?.asString() == list) emptyList()
                else unknown("Unsupported returned value: ${text(result)}")
            else -> unknown("Unsupported return: ${text(result)}")
        }
        is FirPropertyAccessExpression -> {
            val property = (stmt.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty
            if (property?.isLocal == true) emptyList()
            else unknown("Unsupported property access: ${text(stmt)}")
        }
        is FirVariableAssignment -> emptyList() // scalar updates; list reassignment is diagnosed below in parseBlock
        else -> unknown("Unsupported ${stmt::class.simpleName}: ${text(stmt)}")
    }

    private fun parseBlock(block: FirBlock, list: String?, vars: MutableMap<String, String>): List<WorkflowNode> {
        val result = mutableListOf<WorkflowNode>()
        val statements = block.statements
        var i = 0
        while (i < statements.size) {
            val current = statements[i]
            val next = statements.getOrNull(i + 1)
            val iterator = (current as? FirVariable)?.initializer as? FirFunctionCall
            if (iterator?.calleeReference?.name?.asString() == "iterator" && next is FirWhileLoop) {
                val variable = (next.block.statements.firstOrNull() as? FirVariable)?.name?.asString() ?: "i"
                val body = next.block.statements.drop(1).flatMap { parse(it, list, vars.toMutableMap()) }
                result += WorkflowNode.ForEach(variable, text(iterator.explicitReceiver), body)
                i += 2
            } else {
                if (current is FirVariableAssignment && text(current.lValue) == list) {
                    result += WorkflowNode.Unknown("Reassignment of activity list: $list")
                } else result += parse(current, list, vars)
                i++
            }
        }
        return result
    }

    private fun produceList(call: FirFunctionCall, vars: MutableMap<String, String>): List<WorkflowNode> {
        val symbol = (call.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
        val name = call.calleeReference.name.asString()
        val packageName = symbol?.callableId?.packageName?.asString()
        if (packageName == "kotlin.collections") {
            when (name) {
                "buildList" -> {
                    val lambda = call.argumentList.arguments.filterIsInstance<FirAnonymousFunctionExpression>().firstOrNull()
                    return lambda?.anonymousFunction?.body?.let { parse(it, null, vars.toMutableMap()) }
                        ?: unknown("Cannot resolve buildList body")
                }
                "listOf", "mutableListOf" -> return call.argumentList.arguments.flatMap { argument ->
                    if (argument is FirVarargArgumentsExpression) argument.arguments.map {
                        if (it is FirSpreadArgumentExpression) WorkflowNode.Unknown("Spread list argument: ${text(it)}")
                        else WorkflowNode.Step(text(it))
                    }
                    else listOf(WorkflowNode.Step(text(argument)))
                }
                "emptyList" -> return emptyList()
            }
        }
        val target = symbol?.fir?.unwrapFakeOverrides() ?: return unknown("Unresolved list producer: ${text(call)}")
        if (target.valueParameters.isNotEmpty() || target.receiverParameter != null || call.argumentList.arguments.isNotEmpty()) {
            return unknown("List producer with parameters: ${text(call)}")
        }
        val owner = symbol.callableId.classId
        val receiver = call.explicitReceiver
        val isThis = receiver == null || receiver is FirThisReceiverExpression
        val isSuper = (receiver as? FirQualifiedAccessExpression)?.calleeReference is org.jetbrains.kotlin.fir.references.FirSuperReference
        if (owner == null || hierarchy.none { it.symbol.classId == owner } || (!isThis && !isSuper)) {
            return unknown("External list producer: ${text(call)}")
        }
        // Resolve no-argument member hooks in the concrete workflow's class hierarchy.
        val implementation = if (isSuper || target.status.modality == org.jetbrains.kotlin.descriptors.Modality.FINAL) target
        else resolveMethod(target.name) ?: return unknown("Ambiguous member: ${text(call)}")
        return analyze(implementation)
    }

    private fun condition(expression: FirExpression): String = when (expression) {
        is FirBooleanOperatorExpression -> listOf(condition(expression.leftOperand), condition(expression.rightOperand)).joinToString(", ")
        is FirElseIfTrueCondition -> "else"
        else -> text(expression)
    }
}
