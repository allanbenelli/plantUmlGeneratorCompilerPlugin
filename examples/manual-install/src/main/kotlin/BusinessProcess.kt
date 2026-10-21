package com.acme.process

// Your own domain types. No dependency on this plugin is required in source code.
interface BusinessProcess {
    fun steps(): List<Step>
}
interface Step
class ValidateOrder : Step
class ShipOrder : Step

abstract class BaseProcess : BusinessProcess {
    override fun steps(): List<Step> = buildList {
        add(ValidateOrder())
        addAll(customSteps())
    }
    abstract fun customSteps(): List<Step>
}
class OrderProcess : BaseProcess() {
    override fun customSteps(): List<Step> = listOf(ShipOrder())
}
