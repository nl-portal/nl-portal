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
package nl.nlportal.openproduct.service

import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.openproduct.TestHelper
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OPFrontendMenuConditionalCheckerTest(
    @Autowired private val checker: OPFrontendMenuConditionalChecker,
    @Autowired private val openProductModuleConfiguration: OpenProductModuleConfiguration,
) {
    private val authentication = JwtBuilder().aanvragerBsn("569312863").buildBurgerAuthentication()

    @BeforeEach
    fun setUp() {
        down = false
        url = server.url("/").toString()
        openProductModuleConfiguration.properties.productApiUrl = URI(url)
        openProductModuleConfiguration.properties.productTypeApiUrl = URI(url)
    }

    @Test
    fun `holds when the user has products in the theme`() =
        runTest {
            assertTrue(checker.check(authentication, WITH_PRODUCTS))
        }

    @Test
    fun `fails when the user has no products in the theme`() =
        runTest {
            assertFalse(checker.check(authentication, WITHOUT_PRODUCTS))
        }

    @Test
    fun `fails for an unknown theme`() =
        runTest {
            assertFalse(checker.check(authentication, "Onbekend"))
        }

    @Test
    fun `fails when Open Product is down`() =
        runTest {
            down = true

            assertFalse(checker.check(authentication, WITH_PRODUCTS))
        }

    @Test
    fun `fails for an authentication that isn't Common Ground`() =
        runTest {
            assertFalse(checker.check(TestingAuthenticationToken("user", null), WITH_PRODUCTS))
        }

    companion object {
        private const val WITH_PRODUCTS = "Parkeren"
        private const val WITHOUT_PRODUCTS = "Belastingzaken"
        private const val WITH_PRODUCTS_UUID = "41f71c2e-9e0c-4a1b-8d39-709669b256c2"
        private const val EMPTY_PAGE = """{ "count": 0, "next": null, "previous": null, "results": [] }"""

        private val server = MockWebServer()

        @Volatile
        private var down = false
        private var url = ""

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("nl-portal.config.openproduct.properties.product-api-url") { url }
            registry.add("nl-portal.config.openproduct.properties.product-type-api-url") { url }
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
                    if (down) return MockResponse().setResponseCode(503)
                    val query = request.requestUrl!!
                    return when (request.method + " " + request.path?.substringBefore('?')) {
                        "GET /themas" -> {
                            theme(query.queryParameter("naam"))
                        }

                        "GET /producten" -> {
                            if (query.queryParameter("producttype__themas__uuid") == WITH_PRODUCTS_UUID) {
                                TestHelper.mockResponseFromFile("/config/data/get-producten.json")
                            } else {
                                TestHelper.mockResponse(EMPTY_PAGE)
                            }
                        }

                        else -> {
                            MockResponse().setResponseCode(404)
                        }
                    }
                }
            }

        private fun theme(naam: String?): MockResponse {
            val themes = TestHelper.readFileAsString("/config/data/get-themas.json")
            return when (naam) {
                WITH_PRODUCTS -> TestHelper.mockResponse(themes)
                WITHOUT_PRODUCTS -> TestHelper.mockResponse(themes.replace(WITH_PRODUCTS_UUID, "00000000-0000-0000-0000-000000000000"))
                else -> TestHelper.mockResponse(EMPTY_PAGE)
            }
        }
    }
}