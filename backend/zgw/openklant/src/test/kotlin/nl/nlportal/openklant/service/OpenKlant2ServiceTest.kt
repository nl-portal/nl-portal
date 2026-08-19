/*
 * Copyright 2024 Ritense BV, the Netherlands.
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
package nl.nlportal.openklant.service

import java.util.UUID
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.openklant.TestHelper
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration.OpenKlantConfigurationProperties
import nl.nlportal.openklant.client.OpenKlant2KlantinteractiesClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class OpenKlant2ServiceTest {
    private lateinit var openklantModuleConfiguration: OpenKlantModuleConfiguration
    private lateinit var mockServer: MockWebServer
    private lateinit var openKlant2Client: OpenKlant2KlantinteractiesClient
    private lateinit var hostUrl: String
    private lateinit var apiUrl: String

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        hostUrl = "http://${mockServer.hostName}:${mockServer.port}/"
        apiUrl = "http://${mockServer.hostName}:${mockServer.port}/myapi/v1"
        openklantModuleConfiguration =
            OpenKlantModuleConfiguration().apply {
                enabled = true
                properties =
                    OpenKlantConfigurationProperties(
                        klantinteractiesApiUrl = mockServer.url("/myapi/v1").toUri(),
                        token = "SuperSecretToken1234",
                    )
            }

        openKlant2Client = OpenKlant2KlantinteractiesClient(openklantModuleConfiguration.properties)
    }

    @AfterEach
    internal fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `should find KlantContact that belongs to the authenticated user`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            mockServer.enqueue(mockJsonResponse(klantContactenPage))
            mockServer.enqueue(mockJsonResponse(TestHelper.Partijen.klantContact))

            val klantContact =
                service.findKlantContact(
                    authentication = JwtBuilder().aanvragerBsn(OWNER_BSN).buildBurgerAuthentication(),
                    klantContactId = UUID.fromString(KLANTCONTACT_ID),
                )

            assertNotNull(klantContact)
            assertEquals(KLANTCONTACT_ID, klantContact.uuid)

            // the klantcontacten are searched scoped to the authenticated user
            val searchRequest = mockServer.takeRequest()
            assertTrue(searchRequest.path!!.contains("hadBetrokkene__wasPartij__partijIdentificator__objectId=$OWNER_BSN"))
        }

    @Test
    fun `should not find KlantContact of another user`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            // the scoped search of the other user does not return the klantcontact
            mockServer.enqueue(mockJsonResponse(TestHelper.emptyPage))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.findKlantContact(
                        authentication = JwtBuilder().aanvragerBsn(OTHER_BSN).buildBurgerAuthentication(),
                        klantContactId = UUID.fromString(KLANTCONTACT_ID),
                    )
                }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
            // the klantcontact itself is never requested
            assertEquals(1, mockServer.requestCount)
        }

    private fun mockJsonResponse(body: String) =
        MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody(body)

    companion object {
        private const val KLANTCONTACT_ID = "33549ba5-95f0-44d2-9c63-776ec126bc55"
        private const val OWNER_BSN = "296648875"
        private const val OTHER_BSN = "999999999"

        private val klantContactenPage =
            """
            {
                "count": 1,
                "next": null,
                "previous": null,
                "results": [${TestHelper.Partijen.klantContact}]
            }
            """.trimIndent()
    }
}