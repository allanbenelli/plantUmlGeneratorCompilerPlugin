package dev.benelli



class InitialTestWorkflow : WorkflowInterface {
    val case = Case.A
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        activities.add(ActivityA())
        if (true == true) {
            activities.add(ActivityB())
        } else if (false == true) {
            //nothing
        } else {
            activities.add(ActivityC())
        }
        
        if (isFeatureToggle()) activities.add(ActivityD())

        when (case) {
            Case.A -> activities.add(ActivityCaseA())
            Case.B -> activities.add(ActivityCaseB())
            Case.C -> activities.add(ActivityCaseC())
        }
        
        
        return activities
    }
    
    fun isFeatureToggle() : Boolean = false;
}
