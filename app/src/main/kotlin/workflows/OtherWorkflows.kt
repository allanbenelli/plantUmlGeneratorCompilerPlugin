package dev.benelli

class SimpleSimpleWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        activities.add(ActivityA())
        if (true) activities.add(ActivityB()) else activities.add(ActivityC())
        return activities
    }
}



class SimpleWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val steps = mutableListOf<Activity>()
        steps.add(ActivityStart())
        
        if (System.currentTimeMillis() % 2 == 0L) {
            steps.add(ActivityEven())
        } else {
            steps.add(ActivityOdd())
        }
        
        steps.add(ActivityEnd())
        return steps
    }
}

class ComplexIfWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        activities.add(ActivityStart())
        
        val randomValue = (1..10).random()
        if (randomValue > 5) {
            activities.add(ActivityA())
            if (randomValue % 2 == 0) {
                activities.add(ActivityB())
            } else {
                activities.add(ActivityC())
            }
        } else {
            activities.add(ActivityD())
        }
        
        activities.add(ActivityEnd())
        return activities
    }
}

class LoopWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        
        for (i in 1..3) {
            activities.add(ActivityLoop(i))
        }
        
        var counter = 0
        while (counter < 2) {
            activities.add(ActivityWhileLoop(counter))
            counter++
        }
        
        activities.add(ActivityEnd())
        return activities
    }
}

class WhenWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        val status = listOf("NEW", "PROCESSING", "COMPLETED").random()
        
        when (status) {
            "NEW" -> {
                activities.add(ActivityNew())
                activities.add(ActivityInit())
            }
            "PROCESSING" -> activities.add(ActivityProcessing())
            "COMPLETED" -> activities.add(ActivityCompleted())
            else -> activities.add(ActivityUnknown())
        }
        
        return activities
    }
}



class NestedWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        return buildList {
            add(ActivityStart())
            
            if ((1..10).random() > 5) {
                add(ActivityA())
                when ((1..3).random()) {
                    1 -> add(ActivityX())
                    2 -> add(ActivityY())
                    else -> add(ActivityZ())
                }
            } else {
                add(ActivityB())
                for (i in 1..2) {
                    add(ActivityLoop(i))
                }
            }
            
            add(ActivityEnd())
        }
    }
}

class ConditionalBuildListWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        return buildList {
            add(ActivityStart())
            
            if ((1..10).random() > 5) {
                this += ActivityA()
                this += ActivityB()
            } else {
                this += ActivityC()
            }
            
            add(ActivityEnd())
        }
    }
}

class WorkflowWithMultipleWhen : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        val condition = (1..4).random()
        
        when (condition) {
            1, 2 -> activities.add(ActivityCondition12())
            3 -> activities.add(ActivityCondition3())
            else -> activities.add(ActivityConditionElse())
        }
        
        when ((1..2).random()) {
            1 -> activities.add(ActivitySecondWhen1())
            else -> activities.add(ActivitySecondWhen2())
        }
        
        return activities
    }
}

class InfiniteLoopGuardedWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        activities.add(ActivityStart())
        
        var counter = 0
        while (counter < 5) {
            activities.add(ActivityLoop(counter))
            counter++
        }
        
        activities.add(ActivityEnd())
        return activities
    }
}

class AdvancedWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> {
        val activities = mutableListOf<Activity>()
        
        if ((1..2).random() == 1) {
            activities.add(ActivityStart())
        } else {
            activities.add(ActivityAlternativeStart())
        }
        
        when ((1..3).random()) {
            1 -> {
                activities.add(ActivityPath1A())
                activities.add(ActivityPath1B())
            }
            2 -> activities.add(ActivityPath2())
            else -> activities.add(ActivityPath3())
        }
        
        for (i in 1..3) {
            activities.add(ActivityLoop(i))
        }
        
        activities.add(ActivityEnd())
        return activities
    }
}
