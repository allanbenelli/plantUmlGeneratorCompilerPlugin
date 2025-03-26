package dev.benelli

class OtherListNameWorkflow: WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val steps = mutableListOf<Activity>()
        steps.add(ActivityA())
        steps += ActivityB()
        return steps
    }
}


class BuildListWorkflow: WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        return buildList {
            add(ActivityA())
            this += ActivityB()
        }
    }
}


class NestedPlusAssignWorkflow: WorkflowInterface {

    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        activities.add(ActivityA())
        
        if (true)
            activities += ActivityB()
        
        else
            activities.add(ActivityC())
        
        return activities
    }
    
    
}