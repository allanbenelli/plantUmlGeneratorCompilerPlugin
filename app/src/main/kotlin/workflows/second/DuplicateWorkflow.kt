package dev.benelli.second

import dev.benelli.*

class DuplicateWorkflow : WorkflowInterface {
    override fun getActivityList(): List<Activity> = listOf(ActivityB())
}
