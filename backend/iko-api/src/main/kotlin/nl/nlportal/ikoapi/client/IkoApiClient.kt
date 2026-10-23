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
package nl.nlportal.ikoapi.client

import nl.nlportal.core.util.Mapper
import nl.nlportal.ikoapi.client.IkoApiConfig.IkoApiConfigProperties
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import tools.jackson.databind.JsonNode

class IkoApiClient(
    private val webClient: WebClient,
) {
    suspend fun getAggregatedDataProfile(
        name: String,
        id: String,
        containerParams: List<String> = emptyList(),
    ): JsonNode? =
        webClient
            .get()
            .uri { builder ->
                val variables = mutableMapOf<String, Any>("name" to name, "id" to id)
                builder.path("/aggregated-data-profiles/{name}").queryParam("id", "{id}")
                containerParams.forEachIndexed { index, containerParam ->
                    variables["containerParam$index"] = containerParam
                    builder.queryParam("containerParam", "{containerParam$index}")
                }
                builder.build(variables)
            }.retrieve()
            .awaitBodyOrNull<JsonNode>()

    companion object {
        /**
         * Builds a client for the configured IKO instance, authenticated with a client-credentials token from IKO's Keycloak realm.
         *
         * @throws IllegalArgumentException when `url`, `token-url`, `client-id` or `client-secret` is missing
         */
        fun create(
            properties: IkoApiConfigProperties,
            webClientBuilder: WebClient.Builder,
        ): IkoApiClient =
            IkoApiClient(
                webClientBuilder
                    .clone()
                    .baseUrl(requireNotNull(properties.url) { "nl-portal.config.ikoapi.properties.url is required" }.toString().trimEnd('/'))
                    .codecs {
                        with(it.defaultCodecs()) {
                            maxInMemorySize(16 * 1024 * 1024)
                            jacksonJsonEncoder(JacksonJsonEncoder(Mapper.get()))
                            jacksonJsonDecoder(JacksonJsonDecoder(Mapper.get()))
                        }
                    }.filter(
                        IkoClientCredentialsFilter(
                            tokenUrl = requireNotNull(properties.tokenUrl) { "nl-portal.config.ikoapi.properties.token-url is required" },
                            clientId = requireNotNull(properties.clientId) { "nl-portal.config.ikoapi.properties.client-id is required" },
                            clientSecret =
                                requireNotNull(properties.clientSecret) {
                                    "nl-portal.config.ikoapi.properties.client-secret is required"
                                },
                        ),
                    ).build(),
            )
    }
}