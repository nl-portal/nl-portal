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
package nl.nlportal.core.frontend.menu

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.menu.domain.FrontendMenuEntry
import nl.nlportal.core.frontend.menu.domain.FrontendMenuRoute
import org.springframework.security.core.Authentication

/**
 * Serves the configured `nl-portal.frontend.menu-entries` visible to a user's audience.
 * Validates the configuration at construction and fails with one message listing every problem.
 */
class FrontendMenuService(
    private val properties: FrontendConfigurationProperties,
    private val audienceResolver: FrontendMenuAudienceResolver?,
) {
    init {
        val violations = validate(properties.menuEntries)
        check(violations.isEmpty()) {
            "Invalid nl-portal.frontend.menu-entries configuration:\n" + violations.joinToString("\n") { "- $it" }
        }
    }

    fun getMenuEntries(authentication: Authentication): List<FrontendMenuEntry> {
        val audience = audienceResolver?.resolve(authentication) ?: FrontendMenuAudienceResolver.SUBJECT
        return properties.menuEntries
            .filter { it.audience.isEmpty() || it.audience.any { a -> a.equals(audience, ignoreCase = true) } }
            .map { it.toDomain() }
    }

    private fun validate(entries: List<FrontendMenuEntryProperties>): List<String> {
        val duplicateNames =
            entries
                .groupingBy { it.name }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        val duplicatePaths =
            entries
                .groupingBy { it.route.path }
                .eachCount()
                .filterValues { it > 1 }
                .keys

        return entries.flatMapIndexed { index, entry ->
            val route = entry.route
            buildList {
                if (!NAME_PATTERN.matches(entry.name)) add("name must match ${NAME_PATTERN.pattern}")
                if (entry.name in duplicateNames) add("name must be unique")
                if (!route.path.startsWith("/")) add("route.path must start with /")
                if (route.path.contains(":") || route.path.contains("*")) add("route.path must not contain : or *")
                if (route.path in duplicatePaths) add("route.path must be unique")
                if (route.type !in ROUTE_TYPES) add("route.type must be one of ${ROUTE_TYPES.joinToString()}, was '${route.type}'")
                if (route.page.isBlank()) add("route.page must not be blank")
                entry.group?.let { if (it < 0) add("group must be >= 0") }
                entry.ordering?.let { if (it < 0) add("ordering must be >= 0") }
            }.map { "menu-entries[$index] (${entry.name}): $it" }
        }
    }

    private fun FrontendMenuEntryProperties.toDomain() =
        FrontendMenuEntry(
            name = name,
            icon = icon,
            group = group,
            ordering = ordering,
            route =
                FrontendMenuRoute(
                    type = route.type,
                    page = route.page,
                    path = route.path,
                ),
        )

    companion object {
        private val NAME_PATTERN = Regex("^[a-z0-9-]+$")
        val ROUTE_TYPES = listOf("templated", "custom")
    }
}