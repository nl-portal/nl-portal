package nl.nlportal.core.frontend.menu.service

import nl.nlportal.core.frontend.menu.FrontendMenuConditionalChecker

class FrontendMenuConditionalCheckers(
    conditionalCheckers: List<FrontendMenuConditionalChecker>
) {
    val conditionalCheckersMap = conditionalCheckers.groupBy { it.conditionalKey() }.mapValues { it.value.first() }

    fun check(key: String, value: String): Boolean {
        return conditionalCheckersMap[key]?.check(value) ?: true
    }

    fun check(conditions: Map<String, String>): Boolean {
        return conditions.map { check(it.key, it.value) }.all { return it }
    }
}