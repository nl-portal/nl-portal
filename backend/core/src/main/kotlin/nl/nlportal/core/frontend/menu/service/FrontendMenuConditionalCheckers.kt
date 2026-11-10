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

class FrontendMenuConditionalCheckers(
    conditionalCheckers: List<FrontendMenuConditionalChecker>,
) {
    val conditionalCheckersMap = conditionalCheckers.groupBy { it.conditionalKey() }.mapValues { it.value.first() }

    fun check(
        key: String,
        value: String,
    ): Boolean = conditionalCheckersMap[key]?.check(value) ?: true

    fun check(conditions: Map<String, String>): Boolean {
        return conditions.map { check(it.key, it.value) }.all { return it }
    }
}