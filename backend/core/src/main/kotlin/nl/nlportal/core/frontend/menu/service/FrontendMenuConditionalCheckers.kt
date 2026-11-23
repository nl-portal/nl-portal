/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.core.frontend.menu.service

import nl.nlportal.core.frontend.menu.FrontendMenuConditionalChecker
import org.springframework.security.core.Authentication

/**
 * The [FrontendMenuConditionalChecker]s by key. With two checkers for one key the first, in `@Order`, wins.
 */
class FrontendMenuConditionalCheckers(
    checkers: List<FrontendMenuConditionalChecker>,
) {
    private val checkers: Map<String, FrontendMenuConditionalChecker> = checkers.groupBy { it.key }.mapValues { it.value.first() }

    /** Keys that more than one checker claims. */
    val duplicateKeys: Set<String> =
        checkers
            .groupingBy { it.key }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    fun isKnown(key: String): Boolean = key in checkers

    /**
     * Whether every condition in [conditions] holds for the user. An unknown key fails closed.
     */
    suspend fun check(
        authentication: Authentication,
        conditions: Map<String, String>,
    ): Boolean =
        conditions.all { (key, value) ->
            checkers[key]?.check(authentication, value) ?: false
        }
}