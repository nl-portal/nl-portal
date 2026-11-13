/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.frontend.portalpage.binding

import org.springframework.security.core.Authentication
import tools.jackson.databind.JsonNode
import java.util.Locale

/**
 * Supplies data, and optionally a template, for page bindings with `resolver: <type>`.
 *
 * Implementations always scope data to the subject in [PortalPageRequestContext.authentication],
 * treat params and ids as untrusted input, and never return identifiers of other subjects.
 */
interface PortalPageBindingResolver {
    val type: String

    /** Params a page may never list in `overrides`. */
    val fixedParams: Set<String> get() = emptySet()

    /** Problems with the merged params, checked on every request before [resolve]. */
    fun validate(params: PortalPageParams): List<String> = emptyList()

    suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
    ): PortalPageBindingData

    /**
     * Resolves the object [id] for a page requested with an id.
     *
     * @return `null` when the object doesn't exist or isn't owned by the subject
     */
    suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
        id: String,
    ): PortalPageBindingData? = throw UnsupportedOperationException("Resolver '$type' does not support pages requested with an id")
}

data class PortalPageRequestContext(
    val authentication: Authentication,
    val locale: Locale,
)

data class PortalPageBindingData(
    val data: JsonNode,
    val template: String? = null,
)