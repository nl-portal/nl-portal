package nl.nlportal.openproduct.service

import nl.nlportal.core.frontend.menu.FrontendMenuConditionalChecker

class OPFrontendMenuConditionalChecker : FrontendMenuConditionalChecker {
    override fun conditionalKey(): String {
        return "thema"
    }

    override fun check(value: String): Boolean {
        return listOf("wonen-en-boten", "erfpacht").contains(value)
    }
}