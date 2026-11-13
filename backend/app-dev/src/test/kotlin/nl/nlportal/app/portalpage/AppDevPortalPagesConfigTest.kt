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
package nl.nlportal.app.portalpage

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.menu.service.FrontendMenuConditionalCheckers
import nl.nlportal.core.frontend.portalpage.PortalPageConfigValidator
import nl.nlportal.core.frontend.portalpage.PortalPageMenuRouteType
import nl.nlportal.core.frontend.portalpage.PortalPageProjection
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.frontend.portalpage.template.FilePortalPageTemplateSource
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplate
import nl.nlportal.core.util.Mapper
import nl.nlportal.openproduct.portalpage.OpenProductPortalPageBindingResolver
import nl.nlportal.openproduct.service.OPFrontendMenuConditionalChecker
import nl.nlportal.openproduct.service.OpenProductService
import kotlinx.coroutines.test.runTest
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig.ObjectsApiClientConfigProperties
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.portalpage.ObjectPortalPageTemplateSourceProperties
import nl.nlportal.zgw.objectenapi.portalpage.ObjectPortalPageTemplateSource
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import java.io.File

/**
 * The app-dev page examples and menu entries have no config problems, so app-dev mistakes fail the build even though
 * they no longer fail startup. Binding params pass the resolvers' validation,
 * and their templates load with a message for every `i18n` id. Pages and portal page menu entries match one to one,
 * every menu entry has a title in the app-dev frontend and only uses known conditions, and the examples rely on
 * fixtures of the docker-compose setup.
 * Templates use no `raw`, `escape`, `escape_once` or `xml_escape`: output is text by default. They also use no `echo`,
 * `cycle`, `include`, `render` or `layout`, as a tag or in a `liquid` block: those make output part of the template or
 * load another one.
 */
class AppDevPortalPagesConfigTest {
    private val resolvers =
        listOf(
            OpenProductPortalPageBindingResolver(mock(OpenProductService::class.java)),
        )
    private val fileSource = FilePortalPageTemplateSource(listOf("file:src/main/resources/portal-pages/"))
    private val objectSource =
        ObjectPortalPageTemplateSource(
            bindObjectenApi().let { ObjectenApiService(ObjectsApiClient(it), it) },
            bindObjectTemplates(),
        )
    private val validator = PortalPageConfigValidator(resolvers, PortalPageProjection(), listOf(fileSource, objectSource))

    @Test
    fun `page examples are valid`() =
        runTest {
            val properties = bind()

            assertEquals(emptyList<String>(), validator.allProblems(properties.pages))
            properties.pages.forEach { (key, page) ->
                val template = page.template!!
                if (template.source == fileSource.type) assertMessages(key, fileSource.load(PortalPageParams(template.params))!!)
            }
            val pageKeys = properties.pages.keys
            val portalPageEntries = properties.menuEntries.filter { it.route.type == "portal-page" }
            portalPageEntries.forEach { assertEquals(true, it.route.page in pageKeys, "menu entry ${it.name} has no page") }
            pageKeys.forEach { key -> assertTrue(portalPageEntries.any { it.route.page == key }, "page $key has no menu entry") }
        }

    @Test
    fun `menu entries have no problems, so none is left out of the menu`() {
        val properties = bind()
        val menuService =
            FrontendMenuService(
                properties,
                null,
                FrontendMenuConditionalCheckers(listOf(OPFrontendMenuConditionalChecker(mock(OpenProductService::class.java)))),
                listOf(PortalPageMenuRouteType(properties, validator)),
            )

        assertEquals(emptyMap<Int, List<String>>(), menuService.problems())
    }

    @Test
    fun `every menu entry has a title in the app-dev frontend messages and only known conditions`() {
        listOf("nl-nl.ts", "en-gb.ts").forEach { file ->
            val messages = File("$FRONTEND_MESSAGES/$file").readText()
            bind().menuEntries.forEach {
                assertTrue(messages.contains("\"menu-entries.${it.name}\":"), "menu entry ${it.name} has no message in $file")
            }
        }
        bind().menuEntries.forEach { assertTrue(it.conditional.keys.all { key -> key == "thema" }, "menu entry ${it.name}: unknown condition") }
    }

    @Test
    fun `the OpenProduct fixture has a published theme for every thema condition and a content element for every content-label`() {
        val fixtures = Mapper.get().readTree(File(OPENPRODUCT_FIXTURES))
        val themes = fixtures.filter { it.path("model").stringValue() == "producttypen.thema" }.map { it.path("fields").path("naam").stringValue() }
        val labels = fixtures.filter { it.path("model").stringValue() == "producttypen.contentlabel" }.map { it.path("fields").path("naam").stringValue() }
        val properties = bind()

        properties.menuEntries.mapNotNull { it.conditional["thema"] }.forEach { assertTrue(it in themes, "no theme '$it' in the OpenProduct fixture") }
        properties.pages.values
            .flatMap { it.bindings.values }
            .mapNotNull { PortalPageParams(it.params).string("content-label") }
            .forEach { assertTrue(it in labels, "no content label '$it' in the OpenProduct fixture") }
    }

    @Test
    fun `the Open Object fixture has the documented PortaalPaginaTemplate schema and a valid contact object`() {
        val objecttypes = Mapper.get().readTree(File("$FIXTURES/objecttypes.json"))
        val objectType = objecttypes.single { it.path("model").stringValue() == "core.objecttype" && it.path("fields").path("name").stringValue() == "PortaalPaginaTemplate" }
        assertTrue(bindObjectTemplates().objectTypeUrl.endsWith(objectType.path("fields").path("uuid").stringValue()))
        val version =
            objecttypes.single {
                it.path("model").stringValue() == "core.objecttypeversion" && it.path("fields").path("object_type").asInt() == objectType.path("pk").asInt()
            }
        assertEquals(Mapper.get().readTree(File(SCHEMA)), version.path("fields").path("json_schema"))

        val contact =
            Mapper.get().readTree(File("$FIXTURES/objects.json")).single {
                it.path("model").stringValue() == "core.objectrecord" && it.path("fields").path("_object_type").asInt() == objectType.path("pk").asInt()
            }
        assertMessages("contact", PortalPageTemplate.parse(contact.path("fields").path("data")))
    }

    private fun assertMessages(
        page: String,
        template: PortalPageTemplate,
    ) {
        assertEquals(null, UNSAFE_FILTER.find(template.template)?.value, "page $page uses raw or an escape filter")
        assertEquals(null, UNSAFE_TAG.find(template.template)?.value, "page $page uses echo, cycle, include, render or layout")
        val ids = MESSAGE_ID.findAll(template.template).map { it.groupValues[1] }
        listOf("nl-NL", "en-GB").forEach { locale ->
            val messages = template.messages[locale].orEmpty()
            ids.forEach { assertTrue(it in messages, "page $page: message '$it' has no $locale text") }
        }
    }

    private fun bindObjectenApi(): ObjectsApiClientConfigProperties =
        Binder(sources(listOf("application.yml")))
            .bind("nl-portal.config.objectenapi.properties", Bindable.of(ObjectsApiClientConfigProperties::class.java))
            .get()

    private fun bindObjectTemplates(): ObjectPortalPageTemplateSourceProperties =
        Binder(sources(listOf("application.yml")))
            .bind("nl-portal.config.portal-pages.properties.object", Bindable.of(ObjectPortalPageTemplateSourceProperties::class.java))
            .get()

    private fun bind(): FrontendConfigurationProperties = Binder(sources(listOf("application.yml"))).bind("nl-portal.frontend", Bindable.of(FrontendConfigurationProperties::class.java)).get()

    private fun sources(files: List<String>) =
        files.reversed().flatMap { file ->
            YamlPropertySourceLoader().load(file, FileSystemResource("src/main/resources/config/$file")).map { ConfigurationPropertySource.from(it)!! }
        }

    companion object {
        private const val FIXTURES = "../../docker-compose/imports/open-object/fixtures"
        private const val OPENPRODUCT_FIXTURES = "../../docker-compose/imports/openproduct/fixtures/demodata.json"
        private const val FRONTEND_MESSAGES = "../../frontend/packages/app-dev/src/i18n/custom-messages"
        private const val SCHEMA = "../../documentation/features/portaal-pagina-template.schema.json"
        private val UNSAFE_FILTER = Regex("""\|\s*(raw|escape|escape_once|xml_escape)\b""")
        private val UNSAFE_TAG = Regex("""\{%-?\s*(?:liquid\s(?:[^%]*\n)?\s*)?(echo|cycle|include|render|layout)\b""")
        private val MESSAGE_ID = Regex("""'([^']+)'\s*\|\s*i18n""")
    }
}