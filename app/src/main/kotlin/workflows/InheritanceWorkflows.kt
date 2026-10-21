package dev.benelli

open class ParentWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val steps = mutableListOf<Activity>()
        steps.add(ActivityStart())
        steps.add(ActivityEnd())
        return steps
    }
}

class InheritedWorkflow : ParentWorkflow()
class OverriddenWorkflow : ParentWorkflow() {
    override fun getActivityList(): List<Activity> = buildList {
        add(ActivityNew())
    }
}

interface IntermediateWorkflow : WorkflowInterface
class IndirectWorkflow : IntermediateWorkflow {
    override fun getActivityList(): List<Activity> = buildList { add(ActivityA()) }
}

interface DefaultWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = buildList { add(ActivityB()) }
}
class InheritedDefaultWorkflow : DefaultWorkflow

// The old substring matching incorrectly accepted this unrelated interface.
interface NotWorkflowInterface
class UnrelatedWorkflow : NotWorkflowInterface {
    fun getActivityList(): List<Activity> = buildList { add(ActivityError()) }
}

abstract class TemplateWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = buildList {
        add(ActivityStart())
        addAll(customSteps())
        add(ActivityEnd())
    }
    abstract fun customSteps(): List<Activity>
}
class ConcreteTemplateWorkflow : TemplateWorkflow() {
    override fun customSteps(): List<Activity> = listOf(ActivityA(), ActivityB())
}
class SuperCallWorkflow : ParentWorkflow() {
    override fun getActivityList(): List<Activity> = buildList {
        addAll(super.getActivityList())
        add(ActivityNew())
    }
}
class RecursiveWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = getActivityList()
}
class UnsupportedWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = listOf(ActivityA()).filter { true }
}
class MultipleListsWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val unrelated = mutableListOf<Activity>()
        val steps = mutableListOf<Activity>()
        steps.add(ActivityA())
        return steps
    }
}

class CollectionPlusAssignWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = buildList {
        this += listOf(ActivityA(), ActivityB())
    }
}

class EarlyReturnWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val steps = mutableListOf<Activity>()
        if (System.currentTimeMillis() > 0) return steps
        steps.add(ActivityA())
        return steps
    }
}

class SubjectlessWhenWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = buildList {
        when {
            System.currentTimeMillis() < 0 -> add(ActivityA())
            System.currentTimeMillis() == 0L -> add(ActivityB())
            System.currentTimeMillis() > 10 -> add(ActivityC())
            else -> add(ActivityD())
        }
    }
}

interface GeneralDefaultWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = listOf(ActivityA())
}
interface SpecializedDefaultWorkflow : GeneralDefaultWorkflow {
    override fun getActivityList(): List<Activity> = listOf(ActivityB())
}
class MostSpecificDefaultWorkflow : GeneralDefaultWorkflow, SpecializedDefaultWorkflow
