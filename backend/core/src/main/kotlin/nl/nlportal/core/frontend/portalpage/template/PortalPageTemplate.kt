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

import nl.nlportal.core.util.Mapper
import tools.jackson.databind.JsonNode

/**
 * A portal page template: the Liquid [template] with its [messages], keyed by portal locale (`nl-NL`, `en-GB`) and then by message id.
 *
 * Stored as the `PortaalPaginaTemplate` contract, schema 1.0.0 (`documentation/features/portaal-pagina-template.schema.json`):
 * `naam` for editors, `template` and optional `messages` per portal locale. `.portal-page-template.json` files and
 * `PortaalPaginaTemplate` objects in the Objects API use the same format, so templates move between sources unchanged.
 */
data class PortalPageTemplate(
    val template: String,
    val messages: Map<String, Map<String, String>> = emptyMap(),
) {
    companion object {
        val LOCALE_PATTERN = Regex("^[a-z]{2,3}-[A-Z]{2}$")
        private val FIELDS = setOf("naam", "template", "messages")

        /**
         * A template from untyped [messages] as read from a store.
         *
         * @throws IllegalArgumentException when [template] is blank or [messages] isn't `locale → id → string`
         */
        fun of(
            template: String?,
            messages: Any?,
        ): PortalPageTemplate {
            require(!template.isNullOrBlank()) { "template is empty" }
            return PortalPageTemplate(template, messages(messages))
        }

        /**
         * @throws Exception when [json] isn't valid JSON or doesn't follow the `PortaalPaginaTemplate` contract
         */
        fun parse(json: String): PortalPageTemplate = parse(Mapper.get().readTree(json))

        /**
         * @throws IllegalArgumentException when [data] doesn't follow the `PortaalPaginaTemplate` contract
         */
        fun parse(data: JsonNode): PortalPageTemplate {
            require(data.isObject) { "a PortaalPaginaTemplate must be an object" }
            val unknown = data.propertyNames().filter { it !in FIELDS }
            require(unknown.isEmpty()) { "unknown PortaalPaginaTemplate fields: ${unknown.joinToString()}" }
            val name = data.path("naam")
            require(name.isString && name.stringValue().isNotBlank()) { "naam must be a non-empty string" }
            val template = data.path("template")
            require(template.isString) { "template must be a string" }
            val messages = data.get("messages")?.takeUnless { it.isNull }?.let { Mapper.get().convertValue(it, Any::class.java) }
            return of(template.stringValue(), messages)
        }

        private fun messages(value: Any?): Map<String, Map<String, String>> {
            if (value == null) return emptyMap()
            require(value is Map<*, *>) { "messages must be an object of locales" }
            return value.entries.associate { (locale, ids) ->
                require(locale is String && LOCALE_PATTERN.matches(locale)) { "messages locale '$locale' must match ${LOCALE_PATTERN.pattern}" }
                require(ids is Map<*, *>) { "messages.$locale must be an object of message ids" }
                locale to
                    ids.entries.associate { (id, text) ->
                        require(id is String && text is String) { "messages.$locale.$id must be a string" }
                        id to text
                    }
            }
        }
    }
}