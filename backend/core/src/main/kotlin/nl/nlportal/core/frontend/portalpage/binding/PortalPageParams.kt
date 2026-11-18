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

/**
 * Merged binding params as configured, with keys compared canonically: lowercase, dashes removed, dots kept.
 * Values are strings from every config source; a key without a value is absent.
 */
class PortalPageParams(
    values: Map<String, String>,
) {
    private val values: Map<String, String> =
        values
            .filterValues { it.isNotBlank() }
            .mapKeys { canonical(it.key) }

    fun string(key: String): String? = values[canonical(key)]

    /**
     * @throws IllegalArgumentException when the value is not a number
     */
    fun int(key: String): Int? = string(key)?.let { it.trim().toIntOrNull() ?: throw IllegalArgumentException("$key must be a number, was '$it'") }

    /** Indexed keys `key.0`, `key.1`, … in index order, or one comma-separated value, trimmed. */
    fun list(key: String): List<String> {
        val indexed =
            indexedEntries(canonical(key))
                .sortedBy { it.first }
                .map { it.second.trim() }
        val items = indexed.ifEmpty { string(key)?.split(",")?.map { it.trim() } ?: emptyList() }
        return items.filter { it.isNotEmpty() }
    }

    /** Keys under `prefix.`, e.g. all `filter.*`, with the prefix removed. */
    fun withPrefix(prefix: String): Map<String, String> {
        val start = canonical(prefix) + "."
        return values
            .filterKeys { it.startsWith(start) }
            .mapKeys { it.key.removePrefix(start) }
    }

    /**
     * These params with [overrides] applied: a value replaces the key, including its indexed list items;
     * a blank value leaves the configured value in place.
     */
    fun withOverrides(overrides: Map<String, String>): PortalPageParams {
        val merged = values.toMutableMap()
        overrides
            .filterValues { it.isNotBlank() }
            .forEach { (key, value) ->
                val canonicalKey = canonical(key)
                indexedEntries(canonicalKey).forEach { merged.remove("$canonicalKey.${it.first}") }
                merged[canonicalKey] = value
            }
        return PortalPageParams(merged)
    }

    /** The value of [key] as a string: a single value, or list items joined by `, `. */
    fun display(key: String): String? = string(key) ?: list(key).takeIf { it.isNotEmpty() }?.joinToString(", ")

    private fun indexedEntries(canonicalKey: String): List<Pair<Int, String>> =
        values.mapNotNull { (key, value) ->
            key
                .takeIf { it.startsWith("$canonicalKey.") }
                ?.removePrefix("$canonicalKey.")
                ?.toIntOrNull()
                ?.let { it to value }
        }

    override fun equals(other: Any?): Boolean = other is PortalPageParams && other.values == values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "PortalPageParams($values)"

    companion object {
        fun canonical(key: String): String = key.lowercase().replace("-", "")
    }
}