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

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class FilePortalPageTemplateSourceTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `reads a PortaalPaginaTemplate file from the classpath`() =
        runTest {
            val template = FilePortalPageTemplateSource(listOf("classpath:portal-pages/")).load(file("template-a.portal-page-template.json"))!!

            assertEquals("{{ 'titel' | i18n }} {{ items }}\n", template.template)
            assertEquals(
                mapOf("nl-NL" to mapOf("titel" to "Producten", "leeg" to "Geen producten"), "en-GB" to mapOf("titel" to "Products")),
                template.messages,
            )
        }

    @Test
    fun `the first location with the file wins`() =
        runTest {
            Files.writeString(directory.resolve("template-a.portal-page-template.json"), templateJson("eigen"))

            assertEquals("eigen", FilePortalPageTemplateSource(listOf(location(), "classpath:portal-pages/")).load(file("template-a.portal-page-template.json"))!!.template)
            assertEquals("eigen", FilePortalPageTemplateSource(listOf("classpath:missing/", location())).load(file("template-a.portal-page-template.json"))!!.template)
        }

    @Test
    fun `reads from subdirectories`() =
        runTest {
            Files.createDirectories(directory.resolve("producten"))
            Files.writeString(directory.resolve("producten/erfpacht.portal-page-template.json"), templateJson("erfpacht"))

            assertEquals("erfpacht", FilePortalPageTemplateSource(listOf(location())).load(file("producten/erfpacht.portal-page-template.json"))!!.template)
        }

    @Test
    fun `a missing file is null`() =
        runTest {
            assertNull(FilePortalPageTemplateSource(listOf(location(), "classpath:portal-pages/")).load(file("missing.portal-page-template.json")))
        }

    @Test
    fun `only plain relative names ending in portal-page-template json are valid`() {
        val source = FilePortalPageTemplateSource(listOf(location()))

        listOf("over-ons.portal-page-template.json", "producten/erfpacht.portal-page-template.json", "a..b.portal-page-template.json").forEach {
            assertEquals(emptyList<String>(), source.validate(file(it)), it)
        }
        listOf("../application.yaml", "../portal-pages/a.portal-page-template.json", "a/../b.portal-page-template.json", "./a.portal-page-template.json", "/etc/a.portal-page-template.json", "a.yml", "a.json", "a.liquid", "a.portal-page-template.json.yml", "a b.portal-page-template.json", "a\\b.portal-page-template.json")
            .forEach { assertTrue(source.validate(file(it)).single().startsWith("param 'file' must be a relative name"), it) }
        assertEquals(listOf("param 'file' is required"), source.validate(PortalPageParams(emptyMap())))
        assertEquals(listOf("param 'file' is required"), source.validate(file("")))
    }

    @Test
    fun `load refuses an invalid name`() =
        runTest {
            assertThrows<IllegalArgumentException> { FilePortalPageTemplateSource(listOf("classpath:")).load(file("../application.yaml")) }
        }

    @Test
    fun `a symlink out of a file location is not read`() =
        runTest {
            val outside = Files.createDirectories(directory.resolve("outside"))
            val root = Files.createDirectories(directory.resolve("root"))
            Files.writeString(outside.resolve("secret.portal-page-template.json"), templateJson("secret"))
            Files.createSymbolicLink(root.resolve("link.portal-page-template.json"), outside.resolve("secret.portal-page-template.json"))

            assertNull(FilePortalPageTemplateSource(listOf("file:$root/")).load(file("link.portal-page-template.json")))
        }

    @Test
    fun `invalid locations are ignored`() =
        runTest {
            Files.writeString(directory.resolve("a.portal-page-template.json"), templateJson("a"))

            assertEquals("a", FilePortalPageTemplateSource(listOf("ftp:x/", directory.toString(), "classpath:portal-pages", " ${location()} ")).load(file("a.portal-page-template.json"))!!.template)
        }

    @Test
    fun `a file without messages is a template without messages`() {
        assertEquals(PortalPageTemplate("x"), PortalPageTemplate.parse("""{"naam":"a","template":"x"}"""))
        assertEquals(PortalPageTemplate("x"), PortalPageTemplate.parse("""{"naam":"a","template":"x","messages":null}"""))
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
        ).forEach { (case, content) -> assertThrows<Exception>(case) { PortalPageTemplate.parse(content) } }
    }

    @Test
    fun `regional languages are valid locales`() {
        assertEquals(
            setOf("fy-NL", "nds-NL", "pap-BQ"),
            PortalPageTemplate.parse("""{"naam":"a","template":"x","messages":{"fy-NL":{"a":"b"},"nds-NL":{"a":"b"},"pap-BQ":{"a":"b"}}}""").messages.keys,
        )
    }

    private fun location() = "file:$directory/"

    private fun templateJson(template: String) = """{"naam":"$template","template":"$template"}"""

    private fun file(name: String) = PortalPageParams(mapOf("file" to name))
}