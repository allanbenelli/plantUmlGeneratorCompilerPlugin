package dev.benelli


interface WorkflowInterface {
    
    fun getActivityList(): List<Activity>
}

interface Activity

class ActivityA : Activity
class ActivityB : Activity
class ActivityC : Activity
class ActivityD : Activity
class ActivityX : Activity
class ActivityY : Activity
class ActivityZ : Activity

class ActivityCaseA : Activity
class ActivityCaseB : Activity
class ActivityCaseC : Activity

class ActivitySuccess : Activity
class ActivityEnd : Activity
class ActivityStart : Activity

class ActivityEven : Activity
class ActivityOdd : Activity
class ActivityLoop(val iteration: Int) : Activity
class ActivityWhileLoop(val counter: Int) : Activity
class ActivityTry : Activity
class ActivityCatch : Activity
class ActivityFinally : Activity
class ActivityError : Activity
class ActivityCleanup : Activity
class ActivityNew : Activity
class ActivityInit : Activity
class ActivityProcessing : Activity
class ActivityCompleted : Activity
class ActivityUnknown : Activity
class ActivityAlternativeStart : Activity
class ActivityPath1A : Activity
class ActivityPath1B : Activity
class ActivityPath2 : Activity
class ActivityPath3 : Activity
class ActivityCondition12 : Activity
class ActivityCondition3 : Activity
class ActivityConditionElse : Activity
class ActivitySecondWhen1 : Activity
class ActivitySecondWhen2 : Activity


enum class Case {
    A,B,C;
}
