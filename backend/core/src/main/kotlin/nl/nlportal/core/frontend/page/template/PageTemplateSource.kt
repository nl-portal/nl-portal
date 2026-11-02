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
package nl.nlportal.core.frontend.page.template

import nl.nlportal.core.frontend.page.resolver.PageParams

/**
 * Loads template documents for pages with `template.source: <type>`.
 *
 * Template documents are shared content, never subject data: sources load them without the user's authentication.
 */
interface PageTemplateSource {
    val type: String

    /** Problems with the configured params, checked at startup and on every request. */
    fun validate(params: PageParams): List<String> = emptyList()

    /**
     * @return `null` when the document doesn't exist
     * @throws Exception when the store fails or the document is malformed
     */
    suspend fun load(params: PageParams): PageTemplateDocument?
}