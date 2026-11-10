package nl.nlportal.core.frontend.menu

interface FrontendMenuConditionalChecker {

    fun conditionalKey(): String

    fun check(value: String): Boolean

}