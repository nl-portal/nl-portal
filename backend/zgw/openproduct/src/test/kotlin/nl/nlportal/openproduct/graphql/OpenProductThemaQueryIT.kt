/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.openproduct.graphql

import tools.jackson.databind.JsonNode
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.WithBedrijfUser
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.openproduct.TestHelper
import nl.nlportal.openproduct.TestHelper.readFileAsString
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration
import nl.nlportal.core.util.Mapper
import nl.nlportal.zakenapi.client.ZakenApiConfig
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OpenProductThemaQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val openProductModuleConfiguration: OpenProductModuleConfiguration,
    @Autowired private val objectsApiClientConfig: ObjectsApiClientConfig,
    @Autowired private val zakenApiConfig: ZakenApiConfig,
) {
    companion object {
        @JvmStatic
        var server: MockWebServer? = null

        @JvmStatic
        var url: String = ""

        private val requests = CopyOnWriteArrayList<String>()

        private val ZAKEN_BY_NAME =
            """
            query(${'$'}naam: String!) { getOpenProductThemaZakenByName(naam: ${'$'}naam) { uuid, omschrijving } }
            """.trimIndent()

        private val ZAKEN_BY_NAME_PAGED =
            """
            query(${'$'}naam: String!, ${'$'}pageSize: Int) {
                getOpenProductThemaZakenByName(naam: ${'$'}naam, pageSize: ${'$'}pageSize) { uuid }
            }
            """.trimIndent()

        private val TAKEN_BY_NAME =
            """
            query(${'$'}naam: String!, ${'$'}pageSize: Int) {
                getOpenProductThemaTakenByName(naam: ${'$'}naam, pageSize: ${'$'}pageSize) { id }
            }
            """.trimIndent()

        @JvmStatic
        @DynamicPropertySource
        fun properties(propsRegistry: DynamicPropertyRegistry) {
            propsRegistry.add("nl-portal.config.openproduct.properties.product-type-api-url") { url }
            propsRegistry.add("nl-portal.config.openproduct.properties.product-api-url") { url }
            propsRegistry.add("nl-portal.config.zakenapi.properties.url") { url }
            propsRegistry.add("nl-portal.config.objectenapi.properties.url") { url }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server = MockWebServer()
            server?.start()
            url = server?.url("/").toString()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server?.shutdown()
        }
    }

    @BeforeEach
    internal fun setUp() {
        requests.clear()
        setupMockServer()
        url = server?.url("/").toString()
        openProductModuleConfiguration.properties.productTypeApiUrl = URI(url)
        openProductModuleConfiguration.properties.productApiUrl = URI(url)
        objectsApiClientConfig.properties.url = URI(url)
        zakenApiConfig.properties.url = url
    }

    @Test
    @WithBurgerUser("569312863")
    fun `get themas`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemas.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemas")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(4, responseBody.get("totalElements")?.intValue())
            assertEquals("Parkeren", responseBody.requiredAt("/content/0/naam")?.stringValue())
            assertEquals("Parkeervergunning", responseBody.requiredAt("/content/0/producttypen/0/uniformeProductNaam")?.stringValue())
            assertEquals("PARKEREN", responseBody.requiredAt("/content/0/producttypen/0/code")?.stringValue())
        }

    @Test
    @WithBedrijfUser(
        kvkNummer = "569312863",
        machtigingsDienst = "0781d1a1-0644-44fd-802e-96f6b588b472",
    )
    fun `get themas voor bedrijf met machtingsdienst`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemas.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemas")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(4, responseBody.get("totalElements")?.intValue())
            assertEquals("Parkeren", responseBody.requiredAt("/content/0/naam")?.stringValue())
            assertEquals("Parkeervergunning", responseBody.requiredAt("/content/0/producttypen/0/uniformeProductNaam")?.stringValue())
            assertEquals("PARKEREN", responseBody.requiredAt("/content/0/producttypen/0/code")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get hoofd themas`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductHoofdThemas.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductHoofdThemas")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(2, responseBody.size())
            assertEquals("Belastingzaken", responseBody.requiredAt("/0/naam")?.stringValue())
            assertEquals("toeristenbelasting", responseBody.requiredAt("/0/producttypen/0/uniformeProductNaam")?.stringValue())
            assertEquals("BELASTINGZAKEN", responseBody.requiredAt("/0/producttypen/0/code")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get hoofd themas by producten`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductHoofdThemasByProducten.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductHoofdThemasByProducten")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(2, responseBody.size())
            assertEquals("HoofdThema", responseBody.requiredAt("/0/naam")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get themas hierarchy`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemasHierarchy.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemasHierarchy")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(2, responseBody.size())
            assertEquals("Belastingzaken", responseBody.requiredAt("/0/thema/naam")?.stringValue())
            assertEquals("toeristenbelasting", responseBody.requiredAt("/0/thema/producttypen/0/uniformeProductNaam")?.stringValue())
            assertEquals("BELASTINGZAKEN", responseBody.requiredAt("/0/thema/producttypen/0/code")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get themas zaken`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemaZaken.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemaZaken")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(1, responseBody.size())
            assertEquals("Lopende zaak", responseBody.requiredAt("/0/omschrijving")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get themas taken`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemaTaken.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemaTaken")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(2, responseBody.size())
            assertEquals("2d725c07-2f26-4705-8637-438a42b5ac2d", responseBody.requiredAt("/0/id")?.stringValue())
            assertEquals("Taak linked to Zaak", responseBody.requiredAt("/0/titel")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get thema`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThema.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThema")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals("Parkeren", responseBody.requiredAt("/naam")?.stringValue())
            assertEquals("Parkeervergunning", responseBody.requiredAt("/producttypen/0/uniformeProductNaam")?.stringValue())
            assertEquals("PARKEREN", responseBody.requiredAt("/producttypen/0/code")?.stringValue())
            assertEquals("link naar Ritense website", responseBody.requiredAt("/links/0/naam")?.stringValue())
            assertEquals("https://ritense.com/", responseBody.requiredAt("/links/0/url")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get thema hierarchy`() =
        runTest {
            val responseBody =
                httpGraphQlTester
                    .document(readFileAsString("/config/graphql/getOpenProductThemaHierarchy.gql"))
                    .execute()
                    .errors()
                    .verify()
                    .path("getOpenProductThemaHierarchy")
                    .entity(JsonNode::class.java)
                    .get()

            assertEquals(1, responseBody.size())
            assertEquals("HoofdThema", responseBody.requiredAt("/0/thema/naam")?.stringValue())
            assertEquals("Sub thema", responseBody.requiredAt("/0/subThemas/0/thema/naam")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get open zaken of a thema by name`() =
        runTest {
            val responseBody = byName(ZAKEN_BY_NAME, "getOpenProductThemaZakenByName", "Parkeren")

            assertEquals(1, responseBody.size())
            assertEquals("Lopende zaak", responseBody.requiredAt("/0/omschrijving")?.stringValue())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `zaken by name are empty without searching zaken for an unknown thema or one without zaaktypen`() =
        runTest {
            listOf("Onbekend", "Sub thema", "Belastingzaken", " ").forEach { naam ->
                assertEquals(0, byName(ZAKEN_BY_NAME, "getOpenProductThemaZakenByName", naam).size(), naam)
            }
            assertEquals(emptyList<String>(), requests.filter { it.contains("_zoek") })
        }

    @Test
    @WithBurgerUser("569312863")
    fun `get taken of a thema by name, linked to a zaak or product, limited after filtering`() =
        runTest {
            val all = byName(TAKEN_BY_NAME, "getOpenProductThemaTakenByName", "Parkeren")
            val first = byName(TAKEN_BY_NAME, "getOpenProductThemaTakenByName", "Parkeren", pageSize = 1)

            assertEquals(
                listOf("2d725c07-2f26-4705-8637-438a42b5ac2d", "2d725c07-2f26-4705-8637-438a42b5ac2e"),
                all.toList().map { it.path("id").stringValue() },
            )
            assertEquals(listOf("2d725c07-2f26-4705-8637-438a42b5ac2d"), first.toList().map { it.path("id").stringValue() })
            assertEquals(0, byName(TAKEN_BY_NAME, "getOpenProductThemaTakenByName", "Onbekend").size())
        }

    @Test
    @WithBurgerUser("569312863")
    fun `by-name queries reject a page size outside 1 to 100 without calling Open Product`() =
        runTest {
            listOf(0, -1, 101).forEach { pageSize ->
                listOf(TAKEN_BY_NAME to "getOpenProductThemaTakenByName", ZAKEN_BY_NAME_PAGED to "getOpenProductThemaZakenByName").forEach { (document, path) ->
                    httpGraphQlTester
                        .document(document)
                        .variable("naam", "Parkeren")
                        .variable("pageSize", pageSize)
                        .execute()
                        .errors()
                        .expect { it.message == "pageSize must be between 1 and 100" && it.path == path }
                        .verify()
                }
            }
            assertEquals(emptyList<String>(), requests.toList())
        }

    private fun byName(
        document: String,
        path: String,
        naam: String,
        pageSize: Int? = null,
    ): JsonNode =
        httpGraphQlTester
            .document(document)
            .variable("naam", naam)
            .variable("pageSize", pageSize)
            .execute()
            .errors()
            .verify()
            .path(path)
            .entity(JsonNode::class.java)
            .get()

    private fun themas(naam: String?): MockResponse {
        val themas = Mapper.get().readTree(TestHelper.readFileAsString("/config/data/get-themas.json"))
        if (naam == null) return TestHelper.mockResponse(themas.toString())
        val results = themas.path("results").filter { it.path("naam").stringValue() == naam }
        return TestHelper.mockResponse(Mapper.get().writeValueAsString(mapOf("count" to results.size, "next" to null, "previous" to null, "results" to results)))
    }

    private fun productTypeWithoutZaaktypeUrl(): MockResponse {
        val productType = TestHelper.readFileAsString("/config/data/get-producttype.json")
        return TestHelper.mockResponse(productType.replace("\"http://localhost:8001/catalogi/api/v1/744ca059-f412-49d4-8963-5800e4afd486\"", "null"))
    }

    private fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request.path.orEmpty()
                    val path = request.path?.substringBefore('?')
                    val queryParams = request.path?.substringAfter('?')?.split('&') ?: emptyList()
                    val response =
                        when (request.method + " " + path) {
                            "GET /themas/41f71c2e-9e0c-4a1b-8d39-709669b256c2" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-thema.json")
                            }

                            "GET /themas/5422b0e4-18ae-4017-bcea-fb03446a8136" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-subthema.json")
                            }

                            "GET /themas/c111ce6f-308f-4e6d-9460-ffe6a07e283a" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-hoofdthema.json")
                            }

                            "GET /themas" -> {
                                themas(
                                    queryParams
                                        .firstOrNull { it.startsWith("naam=") }
                                        ?.let { URLDecoder.decode(it.removePrefix("naam="), Charsets.UTF_8) },
                                )
                            }

                            "POST /zaken/api/v1/zaken/_zoek" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-zaken.json")
                            }

                            "GET /producttypen/dee273e9-2aa8-40ae-84b7-cb7da3c075ba" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-producttype.json")
                            }

                            "GET /producttypen/202fef9d-4594-45c5-86e4-4bccb06a2679" -> {
                                productTypeWithoutZaaktypeUrl()
                            }

                            "GET /producten" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-producten.json")
                            }

                            "GET /links" -> {
                                TestHelper.mockResponseFromFile("/config/data/get-links.json")
                            }

                            "GET /api/v2/objects" -> {
                                if (queryParams.any { it.contains("identificatie__value__exact__569312863") }) {
                                    TestHelper.mockResponseFromFile("/config/data/get-taken.json")
                                } else {
                                    MockResponse().setResponseCode(404)
                                }
                            }

                            else -> {
                                MockResponse().setResponseCode(404)
                            }
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}