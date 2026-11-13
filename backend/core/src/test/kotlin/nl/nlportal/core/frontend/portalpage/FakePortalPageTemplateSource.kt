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

import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateSource

/**
 * Test template source: templates come from [templates] by param `name`.
 */
class FakePortalPageTemplateSource(
    override val type: String = "fake",
    var templates: Map<String, PortalPageTemplate> = emptyMap(),
) : PortalPageTemplateSource {
    var loads = 0
    var failure: Exception? = null

    override fun validate(params: PortalPageParams): List<String> = if (params.string("name") == null) listOf("param 'name' is required") else emptyList()

    override suspend fun load(params: PortalPageParams): PortalPageTemplate? {
        loads++
        failure?.let { throw it }
        return templates[params.string("name")]
    }
}