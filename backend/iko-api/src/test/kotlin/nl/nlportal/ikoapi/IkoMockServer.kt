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
package nl.nlportal.ikoapi

import nl.nlportal.ikoapi.client.IkoApiClient
import nl.nlportal.ikoapi.client.IkoApiConfig.IkoApiConfigProperties
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One [MockWebServer] serving both IKO's token endpoint (`/token`) and its ADP endpoint (`/api/iko/...`), recording every request.
 */
class IkoMockServer {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()

    var tokenResponse: () -> MockResponse = { token(expiresIn = 300) }
    var adpResponse: () -> MockResponse = { json("{}") }

    init {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests.add(request)
                    return when {
                        request.path == "/token" -> tokenResponse()
                        request.path?.startsWith("/api/iko/") == true -> adpResponse()
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        server.start()
    }

    fun properties(allowedProfiles: List<String> = emptyList()): IkoApiConfigProperties =
        IkoApiConfigProperties().apply {
            url = URI(server.url("/api/iko/").toString())
            tokenUrl = URI(server.url("/token").toString())
            clientId = "nl-portal"
            clientSecret = "secret"
            this.allowedProfiles = allowedProfiles
        }

    fun client(): IkoApiClient = IkoApiClient.create(properties(), WebClient.builder())

    fun tokenRequests(): List<RecordedRequest> = requests.filter { it.path == "/token" }

    fun adpRequests(): List<RecordedRequest> = requests.filter { it.path?.startsWith("/api/iko/") == true }

    fun reset() {
        requests.clear()
        tokenResponse = { token(expiresIn = 300) }
        adpResponse = { json("{}") }
    }

    fun shutdown() {
        server.shutdown()
    }

    companion object {
        fun json(
            body: String,
            status: Int = 200,
        ): MockResponse =
            MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body)

        fun token(expiresIn: Int): MockResponse = json("""{"access_token":"t-1","token_type":"Bearer","expires_in":$expiresIn}""")
    }
}