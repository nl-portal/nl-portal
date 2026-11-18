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

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.frontend.configuration.FrontendBindFailures
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.menu.domain.FrontendMenuEntry
import nl.nlportal.core.frontend.menu.domain.FrontendMenuRoute
import nl.nlportal.core.frontend.menu.service.FrontendMenuConditionalCheckers
import nl.nlportal.core.frontend.portalpage.PortalPageMenuRouteType
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.security.core.Authentication

/**
 * Serves the configured `nl-portal.frontend.menu-entries` visible to a user's audience and whose conditions hold.
 *
 * A misconfigured entry never stops startup: it is left out of the menu, and its problems are logged as errors
 * at startup and after a config refresh only. An entry's `route.type` must be served by a [FrontendMenuRouteType] bean,
 * which a feature only registers while it's enabled.
 */
class FrontendMenuService(
    private val properties: FrontendConfigurationProperties,
    private val audienceResolver: FrontendMenuAudienceResolver?,
    private val conditionalCheckers: FrontendMenuConditionalCheckers,
    routeTypes: List<FrontendMenuRouteType> = emptyList(),
    private val bindFailures: FrontendBindFailures = FrontendBindFailures(),
) {
    private val routeTypes: Map<String, FrontendMenuRouteType> = routeTypes.groupBy { it.type }.mapValues { it.value.first() }
    private val duplicateRouteTypes: Set<String> =
        routeTypes
            .groupingBy { it.type }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    suspend fun getMenuEntries(authentication: Authentication): List<FrontendMenuEntry> {
        val audience = audienceResolver?.resolve(authentication) ?: FrontendMenuAudienceResolver.SUBJECT
        val entries = properties.menuEntries
        val problems = problems(entries)
        return entries
            .filterIndexed { index, _ -> index !in problems }
            .filter { it.audience.isEmpty() || it.audience.any { a -> a.equals(audience, ignoreCase = true) } }
            .filter { conditionalCheckers.check(authentication, it.conditional) }
            .map { it.toDomain() }
    }

    /**
     * Whether a menu entry visible to the user, hidden or not, renders the portal page [page].
     */
    suspend fun isPortalPageVisible(
        authentication: Authentication,
        page: String,
    ): Boolean =
        getMenuEntries(authentication).any {
            it.route.type == PortalPageMenuRouteType.TYPE && PortalPageParams.canonical(it.route.page) == PortalPageParams.canonical(page)
        }

    /**
     * Problems per entry index of the current configuration. Entries with problems are left out of the menu.
     */
    fun problems(): Map<Int, List<String>> = problems(properties.menuEntries)

    /**
     * Logs every configuration problem once, at startup and after a config refresh. Never throws.
     */
    @EventListener(classes = [ApplicationReadyEvent::class, RefreshScopeRefreshedEvent::class])
    fun logProblems() {
        try {
            val entries = properties.menuEntries
            bindFailures.forSection(FrontendBindFailures.MENU_ENTRIES_SECTION).forEach { logger.error { "Invalid nl-portal.frontend.menu-entries: $it" } }
            problems(entries).forEach { (index, problems) ->
                problems.forEach { logger.error { "Invalid nl-portal.frontend.menu-entries[$index] (${entries[index].name}): $it; the entry is left out" } }
            }
            conditionalCheckers.duplicateKeys.forEach { logger.warn { "More than one menu condition checker for key '$it'; the first one is used" } }
            duplicateRouteTypes.forEach { logger.warn { "More than one menu route type '$it'; the first one is used" } }
        } catch (e: Exception) {
            logger.error(e) { "Checking nl-portal.frontend.menu-entries failed" }
        }
    }

    private fun problems(entries: List<FrontendMenuEntryProperties>): Map<Int, List<String>> {
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

        return entries
            .mapIndexed { index, entry ->
                val route = entry.route
                index to
                    buildList {
                        if (!NAME_PATTERN.matches(entry.name)) add("name must match ${NAME_PATTERN.pattern}")
                        if (entry.name in duplicateNames) add("name must be unique")
                        if (!route.path.startsWith("/")) add("route.path must start with /")
                        if (route.path.startsWith("/") && !isValidPath(route.path)) {
                            add("route.path segments must be a literal [A-Za-z0-9._~-]+, a :param or a final *")
                        }
                        if (PARAM_SEGMENT.containsMatchIn(route.path) && !entry.hidden) add("route.path with a :param requires hidden: true")
                        if (route.path in duplicatePaths) add("route.path must be unique")
                        if (route.page.isBlank()) add("route.page must not be blank")
                        entry.group?.let { if (it < 0) add("group must be >= 0") }
                        entry.ordering?.let { if (it < 0) add("ordering must be >= 0") }
                        entry.conditional.keys
                            .filterNot { conditionalCheckers.isKnown(it) }
                            .forEach { add("unknown condition '$it'") }
                        addAll(routeTypeProblems(entry))
                        addAll(bindFailures.forMenuEntry(index))
                    }
            }.filter { it.second.isNotEmpty() }
            .toMap()
    }

    private fun routeTypeProblems(entry: FrontendMenuEntryProperties): List<String> {
        val routeType =
            routeTypes[entry.route.type]
                ?: return listOf("route.type '${entry.route.type}' is not available (unknown, or its feature is disabled)")
        return try {
            routeType.problems(entry)
        } catch (e: Exception) {
            listOf("route.type '${entry.route.type}' check failed: ${e.message}")
        }
    }

    private fun isValidPath(path: String): Boolean {
        val segments = path.removePrefix("/").split("/")
        return segments.withIndex().all { (index, segment) ->
            LITERAL_SEGMENT.matches(segment) || PARAM_SEGMENT.matches(segment) || (segment == "*" && index == segments.lastIndex)
        }
    }

    private fun FrontendMenuEntryProperties.toDomain() =
        FrontendMenuEntry(
            name = name,
            icon = icon,
            group = group,
            ordering = ordering,
            hidden = hidden,
            route =
                FrontendMenuRoute(
                    type = route.type,
                    page = route.page,
                    path = route.path,
                ),
        )

    companion object {
        private val logger = KotlinLogging.logger {}
        private val NAME_PATTERN = Regex("^[a-z0-9-]+$")
        private val LITERAL_SEGMENT = Regex("^[A-Za-z0-9._~-]+$")
        private val PARAM_SEGMENT = Regex("(^|/):[A-Za-z][A-Za-z0-9]*(?=/|$)")
    }
}