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

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.page.resolver.PageParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class FilePageTemplateSourceTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `reads a PortaalPaginaTemplate file from the classpath`() =
        runTest {
            val document = FilePageTemplateSource(listOf("classpath:pages/")).load(file("template-a.pagetemplate.json"))!!

            assertEquals("{{ 'titel' | i18n }} {{ items }}\n", document.template)
            assertEquals(
                mapOf("nl-NL" to mapOf("titel" to "Producten", "leeg" to "Geen producten"), "en-GB" to mapOf("titel" to "Products")),
                document.messages,
            )
        }

    @Test
    fun `the first location with the file wins`() =
        runTest {
            Files.writeString(directory.resolve("template-a.pagetemplate.json"), document("eigen"))

            assertEquals("eigen", FilePageTemplateSource(listOf(location(), "classpath:pages/")).load(file("template-a.pagetemplate.json"))!!.template)
            assertEquals("eigen", FilePageTemplateSource(listOf("classpath:missing/", location())).load(file("template-a.pagetemplate.json"))!!.template)
        }

    @Test
    fun `reads from subdirectories`() =
        runTest {
            Files.createDirectories(directory.resolve("producten"))
            Files.writeString(directory.resolve("producten/erfpacht.pagetemplate.json"), document("erfpacht"))

            assertEquals("erfpacht", FilePageTemplateSource(listOf(location())).load(file("producten/erfpacht.pagetemplate.json"))!!.template)
        }

    @Test
    fun `a missing file is null`() =
        runTest {
            assertNull(FilePageTemplateSource(listOf(location(), "classpath:pages/")).load(file("missing.pagetemplate.json")))
        }

    @Test
    fun `only plain relative names ending in pagetemplate json are valid`() {
        val source = FilePageTemplateSource(listOf(location()))

        listOf("over-ons.pagetemplate.json", "producten/erfpacht.pagetemplate.json", "a..b.pagetemplate.json").forEach {
            assertEquals(emptyList<String>(), source.validate(file(it)), it)
        }
        listOf("../application.yaml", "../pages/a.pagetemplate.json", "a/../b.pagetemplate.json", "./a.pagetemplate.json", "/etc/a.pagetemplate.json", "a.yml", "a.json", "a.liquid", "a.pagetemplate.json.yml", "a b.pagetemplate.json", "a\\b.pagetemplate.json")
            .forEach { assertTrue(source.validate(file(it)).single().startsWith("param 'file' must be a relative name"), it) }
        assertEquals(listOf("param 'file' is required"), source.validate(PageParams(emptyMap())))
        assertEquals(listOf("param 'file' is required"), source.validate(file("")))
    }

    @Test
    fun `load refuses an invalid name`() =
        runTest {
            assertThrows<IllegalArgumentException> { FilePageTemplateSource(listOf("classpath:")).load(file("../application.yaml")) }
        }

    @Test
    fun `a symlink out of a file location is not read`() =
        runTest {
            val outside = Files.createDirectories(directory.resolve("outside"))
            val root = Files.createDirectories(directory.resolve("root"))
            Files.writeString(outside.resolve("secret.pagetemplate.json"), document("secret"))
            Files.createSymbolicLink(root.resolve("link.pagetemplate.json"), outside.resolve("secret.pagetemplate.json"))

            assertNull(FilePageTemplateSource(listOf("file:$root/")).load(file("link.pagetemplate.json")))
        }

    @Test
    fun `invalid locations are ignored`() =
        runTest {
            Files.writeString(directory.resolve("a.pagetemplate.json"), document("a"))

            assertEquals("a", FilePageTemplateSource(listOf("ftp:x/", directory.toString(), "classpath:pages", " ${location()} ")).load(file("a.pagetemplate.json"))!!.template)
        }

    @Test
    fun `a file without messages is a template without messages`() {
        assertEquals(PageTemplateDocument("x"), PortaalPaginaTemplate.parse("""{"naam":"a","template":"x"}"""))
        assertEquals(PageTemplateDocument("x"), PortaalPaginaTemplate.parse("""{"naam":"a","template":"x","messages":null}"""))
    }

    @Test
    fun `files that don't follow the contract throw`() {
        mapOf(
            "invalid json" to """{"naam":"a","template":"x"""",
            "not an object" to """["x"]""",
            "no naam" to """{"template":"x"}""",
            "empty naam" to """{"naam":" ","template":"x"}""",
            "no template" to """{"naam":"a"}""",
            "empty template" to """{"naam":"a","template":" "}""",
            "template not a string" to """{"naam":"a","template":1}""",
            "unknown field" to """{"naam":"a","template":"x","titel":"x"}""",
            "number message" to """{"naam":"a","template":"x","messages":{"nl-NL":{"jaar":2026}}}""",
            "language without region" to """{"naam":"a","template":"x","messages":{"nl":{"titel":"x"}}}""",
            "lowercase region" to """{"naam":"a","template":"x","messages":{"nl-nl":{"titel":"x"}}}""",
        ).forEach { (case, content) -> assertThrows<Exception>(case) { PortaalPaginaTemplate.parse(content) } }
    }

    @Test
    fun `regional languages are valid locales`() {
        assertEquals(
            setOf("fy-NL", "nds-NL", "pap-BQ"),
            PortaalPaginaTemplate.parse("""{"naam":"a","template":"x","messages":{"fy-NL":{"a":"b"},"nds-NL":{"a":"b"},"pap-BQ":{"a":"b"}}}""").messages.keys,
        )
    }

    private fun location() = "file:$directory/"

    private fun document(template: String) = """{"naam":"$template","template":"$template"}"""

    private fun file(name: String) = PageParams(mapOf("file" to name))
}