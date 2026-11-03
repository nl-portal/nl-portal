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
package nl.nlportal.app.page

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.page.PageConfigValidator
import nl.nlportal.core.frontend.page.PageProjection
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.FilePageTemplateSource
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.core.frontend.page.template.PortaalPaginaTemplate
import nl.nlportal.core.util.Mapper
import nl.nlportal.ikoapi.page.IkoPageDataResolver
import nl.nlportal.ikoapi.service.IkoApiService
import nl.nlportal.openproduct.page.OpenProductPageDataResolver
import nl.nlportal.openproduct.service.OpenProductService
import kotlinx.coroutines.test.runTest
import nl.nlportal.zgw.objectenapi.page.ObjectenPageTemplateProperties
import nl.nlportal.zgw.objectenapi.page.ObjectenPageTemplateSource
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource
import java.io.File

/**
 * The app-dev page examples, with and without the `demo` profile, have no config problems,
 * their binding params pass the resolvers' validation, and their templates load with a Dutch message for every id.
 */
class AppDevPagesConfigTest {
    private val resolvers =
        listOf(
            OpenProductPageDataResolver(mock(OpenProductService::class.java)),
            IkoPageDataResolver(mock(IkoApiService::class.java)),
        )
    private val fileSource = FilePageTemplateSource(listOf("file:src/main/resources/pages/"))
    private val objectenSource =
        ObjectenPageTemplateSource(
            mock(ObjectenApiService::class.java),
            bindObjectenTemplates(),
        )
    private val validator = PageConfigValidator(resolvers, PageProjection(), listOf(fileSource, objectenSource))

    @ParameterizedTest
    @ValueSource(strings = ["", "demo"])
    fun `page examples are valid`(profile: String) =
        runTest {
            val properties = bind(profile)

            assertEquals(emptyList<String>(), validator.allProblems(properties.pages))
            properties.pages.forEach { (key, page) ->
                page.bindings.forEach { (alias, binding) ->
                    val resolver = resolvers.single { it.type == binding.resolver }
                    assertEquals(emptyList<String>(), resolver.validate(PageParams(binding.params)), "$key.$alias")
                }
                val template = page.template!!
                if (template.source == fileSource.type) assertMessages(key, fileSource.load(PageParams(template.params))!!)
            }
            val pageKeys = properties.pages.keys
            properties.menuEntries
                .filter { it.route.type == "templated" }
                .forEach { assertEquals(true, it.route.page in pageKeys, "menu entry ${it.name} has no page") }
        }

    @Test
    fun `the Open Object fixture has the documented PortaalPaginaTemplate schema and a valid contact object`() {
        val objecttypes = Mapper.get().readTree(File("$FIXTURES/objecttypes.json"))
        val objectType = objecttypes.single { it.path("model").stringValue() == "core.objecttype" && it.path("fields").path("name").stringValue() == "PortaalPaginaTemplate" }
        assertTrue(bindObjectenTemplates().objectTypeUrl.endsWith(objectType.path("fields").path("uuid").stringValue()))
        val version =
            objecttypes.single {
                it.path("model").stringValue() == "core.objecttypeversion" && it.path("fields").path("object_type").asInt() == objectType.path("pk").asInt()
            }
        assertEquals(Mapper.get().readTree(File(SCHEMA)), version.path("fields").path("json_schema"))

        val contact =
            Mapper.get().readTree(File("$FIXTURES/objects.json")).single {
                it.path("model").stringValue() == "core.objectrecord" && it.path("fields").path("_object_type").asInt() == objectType.path("pk").asInt()
            }
        assertMessages("contact", PortaalPaginaTemplate.parse(contact.path("fields").path("data")))
    }

    private fun assertMessages(
        page: String,
        document: PageTemplateDocument,
    ) {
        val ids = MESSAGE_ID.findAll(document.template).map { it.groupValues[1] } + TITLE_ID.findAll(document.template).map { it.groupValues[1] }
        listOf("nl-NL", "en-GB").forEach { locale ->
            val messages = document.messages[locale].orEmpty()
            ids.forEach { assertTrue(it in messages, "page $page: message '$it' has no $locale text") }
        }
    }

    private fun bindObjectenTemplates(): ObjectenPageTemplateProperties =
        Binder(sources(listOf("application.yml")))
            .bind("nl-portal.config.pages.properties.objecten", Bindable.of(ObjectenPageTemplateProperties::class.java))
            .get()

    private fun bind(profile: String): FrontendConfigurationProperties {
        val files = listOf("application.yml") + listOfNotNull(profile.ifEmpty { null }?.let { "application-$it.yml" })
        return Binder(sources(files)).bind("nl-portal.frontend", Bindable.of(FrontendConfigurationProperties::class.java)).get()
    }

    private fun sources(files: List<String>) =
        files.reversed().flatMap { file ->
            YamlPropertySourceLoader().load(file, FileSystemResource("src/main/resources/config/$file")).map { ConfigurationPropertySource.from(it)!! }
        }

    companion object {
        private const val FIXTURES = "../../docker-compose/imports/open-object/fixtures"
        private const val SCHEMA = "../../documentation/features/portaal-pagina-template.schema.json"
        private val MESSAGE_ID = Regex("""'([^']+)'\s*\|\s*i18n""")
        private val TITLE_ID = Regex("""title: "([^"]+)"""")
    }
}