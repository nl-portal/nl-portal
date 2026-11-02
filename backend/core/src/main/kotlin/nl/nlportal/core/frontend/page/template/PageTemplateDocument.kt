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

/**
 * A page template with its messages, keyed by portal locale (`nl-NL`, `en-GB`) and then by message id.
 */
data class PageTemplateDocument(
    val template: String,
    val messages: Map<String, Map<String, String>> = emptyMap(),
) {
    companion object {
        val LOCALE_PATTERN = Regex("^[a-z]{2,3}-[A-Z]{2}$")

        /**
         * A document from untyped [messages] as read from a store.
         *
         * @throws IllegalArgumentException when [template] is blank or [messages] isn't `locale → id → string`
         */
        fun of(
            template: String?,
            messages: Any?,
        ): PageTemplateDocument {
            require(!template.isNullOrBlank()) { "template is empty" }
            return PageTemplateDocument(template, messages(messages))
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