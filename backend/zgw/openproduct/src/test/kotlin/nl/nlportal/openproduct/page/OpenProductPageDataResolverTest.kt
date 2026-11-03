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
package nl.nlportal.openproduct.page

import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.core.frontend.page.resolver.PageDataContext
import nl.nlportal.core.frontend.page.resolver.PageParams
import nl.nlportal.core.util.Mapper
import nl.nlportal.documentenapi.client.DocumentApisConfig
import nl.nlportal.openproduct.TestHelper
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration
import nl.nlportal.openproduct.service.OpenProductService
import nl.nlportal.zakenapi.client.ZakenApiConfig
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import tools.jackson.databind.node.ObjectNode
import java.net.URI
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OpenProductPageDataResolverTest(
    @Autowired private val resolver: OpenProductPageDataResolver,
    @Autowired private val openProductService: OpenProductService,
    @Autowired private val openProductModuleConfiguration: OpenProductModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
    @Autowired private val zakenApiConfig: ZakenApiConfig,
    @Autowired private val documentApisConfig: DocumentApisConfig,
) {
    @MockitoSpyBean
    lateinit var authenticationMachtigingsDienstService: AuthenticationMachtigingsDienstService

    @BeforeEach
    fun setUp() {
        down = false
        requests.clear()
        url = server.url("/").toString()
        openProductModuleConfiguration.properties.productApiUrl = URI(url)
        openProductModuleConfiguration.properties.productTypeApiUrl = URI(url)
        objectsApiClientConfig.properties.url = URI(url)
        zakenApiConfig.properties.url = url
        documentApisConfig.properties.getConfig("openzaak").url = url
    }

    private fun owner() = context(OWNER_BSN)

    private fun params() = PageParams(emptyMap())

    private fun garbageParams() =
        listOf(
            PageParams(mapOf("page" to "-1")),
            PageParams(mapOf("page" to "een")),
            PageParams(mapOf("size" to "0")),
            PageParams(mapOf("size" to "101")),
            PageParams(mapOf("filter.status" to "kapot")),
        )

    @Test
    fun `by-id returns null for a non-owner`() =
        runTest {
            assertNull(resolver.resolve(context(NON_OWNER_BSN), params(), OWNED))
        }

    @Test
    fun `an outage throws`() {
        down = true

        assertThrows<Exception> { runTest { resolver.resolve(owner(), params()) } }
        assertThrows<Exception> { runTest { resolver.resolve(owner(), params(), OWNED) } }
    }

    @Test
    fun `output never contains identifiers of other subjects`() =
        runTest {
            val outputs = listOf(resolver.resolve(owner(), params()).data.toString(), resolver.resolve(owner(), params(), OWNED)?.data.toString())

            listOf(OTHER_BSN, OWNER_BSN).forEach { identifier ->
                outputs.forEach { output -> assertFalse(output.contains(identifier), "Found '$identifier' in $output") }
            }
        }

    @Test
    fun `validate rejects garbage`() {
        garbageParams().forEach { params ->
            val problems =
                try {
                    resolver.validate(params)
                } catch (e: IllegalArgumentException) {
                    listOf(e.message)
                }
            assertTrue(problems.isNotEmpty(), "Expected problems for $params")
        }
    }

    @Test
    fun `list maps 0-based page, size, status and product type codes onto the query`() =
        runTest {
            val params =
                PageParams(
                    mapOf(
                        "page" to "2",
                        "size" to "5",
                        "filter.status" to "Actief",
                        "product-type-code" to "PARKEREN",
                        "product-type-codes.0" to "PARKEREN",
                        "product-type-codes.1" to "ERFPACHT",
                    ),
                )

            resolver.resolve(owner(), params)

            val query = requests.single { it.path!!.startsWith("/producten?") }.requestUrl!!
            assertEquals("3", query.queryParameter("page"))
            assertEquals("5", query.queryParameter("page_size"))
            assertEquals("actief", query.queryParameter("status"))
            assertEquals("PARKEREN", query.queryParameter("producttype__code"))
            assertEquals("PARKEREN,ERFPACHT", query.queryParameter("producttype__code__in"))
            assertEquals(OWNER_BSN, query.queryParameter("eigenaren__bsn"))
            assertEquals("true", query.queryParameter("gepubliceerd"))
        }

    @Test
    fun `list defaults to the first page of 10 without status filter`() =
        runTest {
            resolver.resolve(owner(), params())

            val query = requests.single { it.path!!.startsWith("/producten?") }.requestUrl!!
            assertEquals("1", query.queryParameter("page"))
            assertEquals("10", query.queryParameter("page_size"))
            assertNull(query.queryParameter("status"))
        }

    @Test
    fun `list returns the raw result page without owners`() =
        runTest {
            val data = resolver.resolve(owner(), params()).data

            assertEquals(4, data.get("count").intValue())
            assertEquals("694242af-d906-470b-b7e1-eb3527886854", data.requiredAt("/results/0/uuid").stringValue())
            assertEquals("2025-04-30", data.requiredAt("/results/0/start_datum").stringValue())
            assertTrue(data.get("results").all { !it.has("eigenaren") })
        }

    @Test
    fun `validate accepts valid params`() {
        assertEquals(emptyList<String>(), resolver.validate(PageParams(mapOf("page" to "0", "size" to "100", "filter.status" to "in_aanvraag"))))
    }

    @Test
    fun `by-id returns the owned product without owners`() =
        runTest {
            val data = resolver.resolve(owner(), params(), OWNED)!!

            assertEquals(OWNED, data.data.get("uuid").stringValue())
            assertFalse(data.data.has("eigenaren"))
            assertNull(data.template)
        }

    @Test
    fun `by-id of an invalid uuid is null without calling Open Product`() =
        runTest {
            assertNull(resolver.resolve(owner(), params(), "../producten"))
            assertEquals(emptyList<RecordedRequest>(), requests.toList())
        }

    @Test
    fun `by-id with another product type code is null`() =
        runTest {
            assertNull(resolver.resolve(owner(), PageParams(mapOf("product-type-code" to "ERFPACHT")), OWNED))
            assertNull(resolver.resolve(owner(), PageParams(mapOf("product-type-codes" to "ERFPACHT, WOONWAGEN")), OWNED))
            assertNotNull(resolver.resolve(owner(), PageParams(mapOf("product-type-codes" to "ERFPACHT, PARKEREN")), OWNED))
        }

    @Test
    fun `by-id picks the template of the content label in the requested language`() =
        runTest {
            val params = PageParams(mapOf("content-label" to "klantportaal"))

            assertEquals("NL {{ naam }}", resolver.resolve(owner(), params, OWNED)!!.template)
            assertEquals("EN {{ naam }}", resolver.resolve(context(OWNER_BSN, Locale.ENGLISH), params, OWNED)!!.template)

            val contentRequest = requests.last { it.path!!.startsWith("/content") }
            assertEquals("en", contentRequest.getHeader("Accept-Language"))
            assertEquals(PARKEREN_TYPE, contentRequest.requestUrl!!.queryParameter("producttype__uuid"))
        }

    @Test
    fun `by-id without an element with the content label has no template`() =
        runTest {
            assertNull(resolver.resolve(owner(), PageParams(mapOf("content-label" to "onbekend")), OWNED)!!.template)
        }

    @Test
    fun `findOwnedProduct returns null for an unpublished product`() =
        runTest {
            assertNull(openProductService.findOwnedProduct(owner().authentication as CommonGroundAuthentication, UUID.fromString(UNPUBLISHED)))
        }

    @Test
    fun `findOwnedProduct returns null for a product type the machtigingsdienst doesn't allow`() =
        runTest {
            doReturn(false).whenever(authenticationMachtigingsDienstService).isAllowedProductType(any(), any())

            assertNull(resolver.resolve(owner(), params(), OWNED))
        }

    @Test
    fun `findOwnedProduct returns null on 404 and throws on 500`() =
        runTest {
            assertNull(resolver.resolve(owner(), params(), MISSING))
            assertThrows<Exception> { resolver.resolve(owner(), params(), BROKEN) }
        }

    private fun context(
        bsn: String,
        locale: Locale = Locale.forLanguageTag("nl"),
    ) = PageDataContext(JwtBuilder().aanvragerBsn(bsn).buildBurgerAuthentication(), locale)

    companion object {
        private const val OWNER_BSN = "569312863"
        private const val NON_OWNER_BSN = "999993653"
        private const val OTHER_BSN = "14127293"
        private const val OWNED = "694242af-d906-470b-b7e1-eb3527886854"
        private const val UNPUBLISHED = "11111111-1111-1111-1111-111111111111"
        private const val MISSING = "22222222-2222-2222-2222-222222222222"
        private const val BROKEN = "33333333-3333-3333-3333-333333333333"
        private const val PARKEREN_TYPE = "dee273e9-2aa8-40ae-84b7-cb7da3c075ba"

        private val server = MockWebServer()
        private val requests = CopyOnWriteArrayList<RecordedRequest>()

        @Volatile
        private var down = false
        private var url = ""

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("nl-portal.config.openproduct.properties.product-api-url") { url }
            registry.add("nl-portal.config.openproduct.properties.product-type-api-url") { url }
            registry.add("nl-portal.config.zakenapi.properties.url") { url }
            registry.add("nl-portal.config.objectenapi.properties.url") { url }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server.dispatcher = dispatcher()
            server.start()
            url = server.url("/").toString()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server.shutdown()
        }

        private fun dispatcher() =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    if (down) return MockResponse().setResponseCode(503)
                    return when (request.method + " " + request.path?.substringBefore('?')) {
                        "GET /producten" -> TestHelper.mockResponseFromFile("/config/data/get-producten.json")
                        "GET /producten/$OWNED" -> TestHelper.mockResponseFromFile("/config/data/get-product.json")
                        "GET /producten/$UNPUBLISHED" -> TestHelper.mockResponse(unpublishedProduct())
                        "GET /producten/$BROKEN" -> MockResponse().setResponseCode(500)
                        "GET /content" -> TestHelper.mockResponse(contentElements(request.getHeader("Accept-Language") ?: "nl"))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }

        private fun unpublishedProduct(): String {
            val product = Mapper.get().readTree(TestHelper.readFileAsString("/config/data/get-product.json")) as ObjectNode
            product.put("uuid", UNPUBLISHED)
            product.put("gepubliceerd", false)
            return product.toString()
        }

        private fun contentElements(language: String): String {
            val prefix = language.uppercase()
            return """
                {
                  "count": 2,
                  "results": [
                    { "uuid": "bcc4ba4e-8022-447f-8ffd-d65781a51d24", "taal": "$language", "content": "andere inhoud", "labels": ["website"] },
                    { "uuid": "bcc4ba4e-8022-447f-8ffd-d65781a51d25", "taal": "$language", "content": "$prefix {{ naam }}", "labels": ["website", "klantportaal"] }
                  ]
                }
                """.trimIndent()
        }
    }
}