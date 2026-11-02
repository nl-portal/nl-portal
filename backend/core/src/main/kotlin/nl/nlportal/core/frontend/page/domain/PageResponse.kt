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
package nl.nlportal.core.frontend.page.domain

import tools.jackson.databind.JsonNode

/**
 * @property messages the template's messages, keyed by portal locale and then by message id
 * @property root the alias whose data is the template's root when the template comes from a binding;
 * [template] is then `null` when the binding's resolver supplied none
 */
data class PageResponse(
    val type: String,
    val template: String?,
    val messages: Map<String, Map<String, String>>,
    val root: String?,
    val bindings: List<BindingResponse>,
)

/**
 * One resolved binding. [data] holds only the projected data; it is `null` when [error] is set.
 */
data class BindingResponse(
    val alias: String,
    val data: JsonNode?,
    val error: Boolean,
    val meta: BindingMeta,
)

/**
 * @property params the effective values of the binding's `overrides` keys, keyed as written in `overrides`
 */
data class BindingMeta(
    val params: Map<String, String>,
)

data class BindingRefetchRequest(
    val params: Map<String, String> = emptyMap(),
)