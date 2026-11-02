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
package nl.nlportal.zgw.objectenapi.page

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.frontend.page.template.PageTemplateDocument
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig.ObjectsApiClientConfigProperties
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ObjectenPageTemplateSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: ObjectenPageTemplateSource

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    when (request.path) {
                        "/api/v2/objects/$CONTACT" -> json(templateObject(TYPE, """"template":"{{ 'titel' | i18n }}","messages":{"nl-NL":{"titel":"Contact"},"en-GB":{"titel":"Contact us"}}"""))
                        "/api/v2/objects/$NO_MESSAGES" -> json(templateObject("$TYPE/", """"template":"x""""))
                        "/api/v2/objects/$TAAK" -> json(templateObject("http://localhost/api/v2/objecttypes/taak", """"template":"x""""))
                        "/api/v2/objects/$EMPTY" -> json(templateObject(TYPE, """"template":" """"))
                        "/api/v2/objects/$BAD_LOCALE" -> json(templateObject(TYPE, """"template":"x","messages":{"nl":{"titel":"x"}}"""))
                        "/api/v2/objects/$BROKEN" -> MockResponse().setResponseCode(500)
                        else -> MockResponse().setResponseCode(404)
                    }
            }
        server.start()
        source = source(TYPE)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `loads the template and messages of an object`() =
        runTest {
            assertEquals(
                PageTemplateDocument("{{ 'titel' | i18n }}", mapOf("nl-NL" to mapOf("titel" to "Contact"), "en-GB" to mapOf("titel" to "Contact us"))),
                source.load(objectParam(CONTACT)),
            )
        }

    @Test
    fun `messages are optional and the objecttype is compared without trailing slash`() =
        runTest {
            assertEquals(PageTemplateDocument("x"), source.load(objectParam(NO_MESSAGES)))
        }

    @Test
    fun `a missing object or an object of another type is null`() =
        runTest {
            assertNull(source.load(objectParam("00000000-0000-0000-0000-000000000000")))
            assertNull(source.load(objectParam(TAAK)))
        }

    @Test
    fun `other errors and malformed objects throw`() =
        runTest {
            assertThrows<Exception> { source.load(objectParam(BROKEN)) }
            assertThrows<IllegalArgumentException> { source.load(objectParam(EMPTY)) }
            assertThrows<IllegalArgumentException> { source.load(objectParam(BAD_LOCALE)) }
        }

    @Test
    fun `object must be a UUID and the objecttype configured`() {
        assertEquals(emptyList<String>(), source.validate(objectParam(CONTACT)))
        assertEquals(listOf("param 'object' must be a UUID"), source.validate(objectParam("contact")))
        assertEquals(listOf("param 'object' must be a UUID"), source.validate(PageParams(emptyMap())))
        assertEquals(listOf("nl-portal.config.pages.properties.objecten.object-type-url is not configured"), source("").validate(objectParam(CONTACT)))
    }

    private fun source(objectTypeUrl: String): ObjectenPageTemplateSource {
        val clientProperties = ObjectsApiClientConfigProperties(url = server.url("/").toUri(), token = "token")
        return ObjectenPageTemplateSource(
            ObjectenApiService(ObjectsApiClient(clientProperties), clientProperties),
            ObjectenPageTemplateProperties().apply { this.objectTypeUrl = objectTypeUrl },
        )
    }

    private fun objectParam(id: String) = PageParams(mapOf("object" to id))

    private fun templateObject(
        type: String,
        data: String,
    ) = """{"uuid":"$CONTACT","type":"$type","record":{"index":1,"typeVersion":1,"startAt":"2026-10-30","data":{"naam":"Contact",$data}}}"""

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    companion object {
        private const val TYPE = "http://localhost/api/v2/objecttypes/portaal-pagina-template"
        private const val CONTACT = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b01"
        private const val NO_MESSAGES = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b02"
        private const val TAAK = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b03"
        private const val EMPTY = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b04"
        private const val BAD_LOCALE = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b05"
        private const val BROKEN = "7c1e1f7a-2b8e-4a55-9c43-0e6f2d6a1b06"
    }
}