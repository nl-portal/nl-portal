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
package nl.nlportal.core.frontend.portalpage.template

import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams

/**
 * Loads portal page templates through the [sources], on every request: a changed template shows at the next load.
 */
class PortalPageTemplateLoader(
    val sources: List<PortalPageTemplateSource>,
) {
    /**
     * @return `null` when the template doesn't exist
     * @throws Exception when the source isn't provided by exactly one bean, the store fails or the template is malformed
     */
    suspend fun load(
        type: String,
        params: Map<String, String>,
    ): PortalPageTemplate? = sources.single { it.type == type }.load(PortalPageParams(params))
}