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
package nl.nlportal.core.frontend.portalpage

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties
import nl.nlportal.core.frontend.menu.FrontendMenuRouteType
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams

/**
 * The `portal-page` menu route type, registered only while portal pages are enabled. An entry is accepted when its
 * `route.page` names a valid page; binding problems don't count, because the page still renders without that binding.
 *
 * Never depends on `FrontendMenuService` or `PortalPageService`: both depend on it.
 */
class PortalPageMenuRouteType(
    private val properties: FrontendConfigurationProperties,
    private val validator: PortalPageConfigValidator,
) : FrontendMenuRouteType {
    override val type = TYPE

    override fun problems(entry: FrontendMenuEntryProperties): List<String> =
        buildList {
            if (entry.route.path.endsWith("/*")) add("route.path may not end in /* for route.type $TYPE")
            if (entry.route.page.isBlank()) return@buildList
            val key = properties.pages.keys.firstOrNull { PortalPageParams.canonical(it) == PortalPageParams.canonical(entry.route.page) }
            when {
                key == null -> add("page '${entry.route.page}' is not configured in pages")
                validator.pageProblems(key, properties.pages.getValue(key), properties.pages.keys).isNotEmpty() -> add("page '$key' is invalid, see pages.$key")
            }
        }

    companion object {
        const val TYPE = "portal-page"
    }
}