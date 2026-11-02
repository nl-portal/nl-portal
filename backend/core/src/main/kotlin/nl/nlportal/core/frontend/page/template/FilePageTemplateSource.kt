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

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.nlportal.core.frontend.page.resolver.PageParams
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader

/**
 * Reads `.pagetemplate.json` files from `classpath:` or `file:` [locations]; the first location with the file wins.
 *
 * A file follows the [PortaalPaginaTemplate] contract, the same as a template object in the Objects API.
 * Only plain relative names ending in `.pagetemplate.json` are read, and `file:` reads never leave their location,
 * so a page can't load other files such as Spring configuration.
 */
class FilePageTemplateSource(
    locations: List<String>,
    private val resourceLoader: ResourceLoader = DefaultResourceLoader(),
) : PageTemplateSource {
    override val type = "file"

    private val locations: List<String> =
        locations.map { it.trim() }.filter { location ->
            val valid = location.endsWith("/") && (location.startsWith(CLASSPATH) || location.startsWith(FILE))
            if (!valid) logger.warn { "Ignoring page template location '$location': it must start with $CLASSPATH or $FILE and end with /" }
            valid
        }

    override fun validate(params: PageParams): List<String> =
        buildList {
            val name = params.string(FILE_PARAM)
            when {
                name == null -> {
                    add("param '$FILE_PARAM' is required")
                }

                !NAME_PATTERN.matches(name) || name.split("/").any { it.all { char -> char == '.' } } -> {
                    add("param '$FILE_PARAM' must be a relative name of ${NAME_PATTERN.pattern} without '..'")
                }
            }
        }

    override suspend fun load(params: PageParams): PageTemplateDocument? {
        val name = params.string(FILE_PARAM)
        require(name != null && validate(params).isEmpty()) { "invalid param '$FILE_PARAM'" }
        return withContext(Dispatchers.IO) {
            locations.firstNotNullOfOrNull { resource(it, name) }?.let { PortaalPaginaTemplate.parse(it.inputStream.use { stream -> stream.readAllBytes() }.decodeToString()) }
        }
    }

    private fun resource(
        location: String,
        name: String,
    ): Resource? {
        val resource = resourceLoader.getResource(location + name)
        if (!resource.exists() || !resource.isReadable) return null
        if (location.startsWith(FILE)) {
            val root =
                resourceLoader
                    .getResource(location)
                    .file
                    .toPath()
                    .toRealPath()
            val path = resource.file.toPath().toRealPath()
            if (!path.startsWith(root)) {
                logger.warn { "Ignoring page template '$name': it resolves outside location '$location'" }
                return null
            }
        }
        return resource
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val CLASSPATH = "classpath:"
        private const val FILE = "file:"
        private const val FILE_PARAM = "file"
        private val NAME_PATTERN = Regex("^[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*\\.pagetemplate\\.json$")
    }
}