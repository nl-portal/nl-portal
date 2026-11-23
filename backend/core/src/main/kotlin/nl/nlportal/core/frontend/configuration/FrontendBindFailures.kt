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
package nl.nlportal.core.frontend.configuration

import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor
import org.springframework.boot.context.properties.bind.AbstractBindHandler
import org.springframework.boot.context.properties.bind.BindContext
import org.springframework.boot.context.properties.bind.BindHandler
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.source.ConfigurationPropertyName
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps values Spring can't bind under `nl-portal.frontend.menu-entries` and `nl-portal.frontend.pages` from stopping startup.
 * Such a failure is recorded and the field keeps its default; the menu entry or page it belongs to then counts as broken,
 * so a default is never trusted. Failures anywhere else still fail binding. The record is cleared whenever
 * `nl-portal.frontend` is bound again, e.g. on a config refresh.
 */
class FrontendBindFailures : ConfigurationPropertiesBindHandlerAdvisor {
    private val failures = ConcurrentHashMap<String, String>()

    override fun apply(bindHandler: BindHandler): BindHandler =
        object : AbstractBindHandler(bindHandler) {
            override fun <T : Any> onStart(
                name: ConfigurationPropertyName,
                target: Bindable<T>,
                context: BindContext,
            ): Bindable<T>? {
                if (name == ROOT) failures.clear()
                return super.onStart(name, target, context)
            }

            override fun onFailure(
                name: ConfigurationPropertyName,
                target: Bindable<*>,
                context: BindContext,
                error: Exception,
            ): Any? {
                if (listOf(MENU_ENTRIES, PAGES).none { it == name || it.isAncestorOf(name) }) return super.onFailure(name, target, context, error)
                record(name.toString(), error.cause?.message ?: error.message ?: error.javaClass.simpleName)
                return null
            }
        }

    internal fun record(
        name: String,
        message: String,
    ) {
        failures[name] = message
    }

    /** Problems of `menu-entries[index]`. */
    fun forMenuEntry(index: Int): List<String> = matching { it.startsWith("$MENU_ENTRIES[$index].") || it == "$MENU_ENTRIES[$index]" }

    /** Problems of `pages.<key>`, its bindings included; keys are compared canonically. */
    fun forPage(key: String): List<String> =
        matching { name ->
            name.startsWith("$PAGES.") &&
                PortalPageParams.canonical(name.removePrefix("$PAGES.").substringBefore('.').substringBefore('[')) == PortalPageParams.canonical(key)
        }

    /** Problems of `menu-entries` or `pages` as a whole, e.g. a single value where a list or map is expected. */
    fun forSection(section: String): List<String> = matching { it == "$ROOT.$section" }

    private fun matching(predicate: (String) -> Boolean): List<String> =
        failures.entries
            .filter { predicate(it.key) }
            .sortedBy { it.key }
            .map { "could not read ${it.key.removePrefix("$ROOT.")}: ${it.value}" }

    companion object {
        private val ROOT = ConfigurationPropertyName.of("nl-portal.frontend")
        private val MENU_ENTRIES = ConfigurationPropertyName.of("nl-portal.frontend.menu-entries")
        private val PAGES = ConfigurationPropertyName.of("nl-portal.frontend.pages")
        const val MENU_ENTRIES_SECTION = "menu-entries"
        const val PAGES_SECTION = "pages"
    }
}