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
package nl.nlportal.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Jackson 2 and jackson-jq are on the classpath only for jq projections, so only `PageProjection.kt`
 * may import them. Everything else uses Jackson 3 (`tools.jackson`).
 */
class JacksonImportRuleTest {
    @Test
    fun `Jackson 2 and jackson-jq are only imported by PageProjection`() {
        val backend = File("..").canonicalFile
        val sources =
            backend
                .walkTopDown()
                .onEnter { it.name !in EXCLUDED_DIRECTORIES }
                .filter { it.isFile && it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/kotlin/") }
                .toList()

        assertTrue(sources.size > 100, "Scanned only ${sources.size} files under $backend")

        val violations =
            sources
                .filter { it.name !in ALLOWED_FILES }
                .flatMap { file ->
                    file
                        .readLines()
                        .filter { line -> FORBIDDEN_IMPORTS.any { line.trim().startsWith("import $it") } }
                        .map { "${file.relativeTo(backend)}: ${it.trim()}" }
                }

        assertEquals(emptyList<String>(), violations)
    }

    companion object {
        private val FORBIDDEN_IMPORTS = listOf("com.fasterxml.jackson.databind", "com.fasterxml.jackson.core", "net.thisptr")
        private val ALLOWED_FILES = setOf("PageProjection.kt")
        private val EXCLUDED_DIRECTORIES = setOf("build", "out", ".gradle", "node_modules")
    }
}