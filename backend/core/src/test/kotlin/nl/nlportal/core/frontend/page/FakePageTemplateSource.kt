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
package nl.nlportal.core.frontend.page

import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.core.frontend.page.template.PageTemplateSource

/**
 * Test template source: documents come from [documents] by param `name`.
 */
class FakePageTemplateSource(
    override val type: String = "fake",
    var documents: Map<String, PageTemplateDocument> = emptyMap(),
) : PageTemplateSource {
    var loads = 0
    var failure: Exception? = null

    override fun validate(params: PageParams): List<String> = if (params.string("name") == null) listOf("param 'name' is required") else emptyList()

    override suspend fun load(params: PageParams): PageTemplateDocument? {
        loads++
        failure?.let { throw it }
        return documents[params.string("name")]
    }
}