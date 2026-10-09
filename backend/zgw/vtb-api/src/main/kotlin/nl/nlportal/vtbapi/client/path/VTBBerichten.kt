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
package nl.nlportal.vtbapi.client.path

import java.util.UUID
import nl.nlportal.vtbapi.client.VTBClient
import nl.nlportal.vtbapi.client.domain.ResultPage
import nl.nlportal.vtbapi.client.domain.VTBBericht
import nl.nlportal.vtbapi.client.domain.VTBBerichtFilters
import nl.nlportal.vtbapi.client.domain.VTBBerichtUpdate
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBody

class VTBBerichten(
    val client: VTBClient,
) : VTBPath() {
    override val path: String = "/berichten/api/v1/berichten"

    suspend fun get(searchFilters: List<Pair<VTBBerichtFilters, Any>>? = null): ResultPage<VTBBericht> =
        client
            .webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(path)
                    .applyFilters(searchFilters)
                uriBuilder.build()
            }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()

    suspend fun get(id: UUID): VTBBericht =
        client
            .webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("$path/$id")
                    .build()
            }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()

    suspend fun patch(
        id: UUID,
        berichtUpdate: VTBBerichtUpdate,
    ): VTBBericht =
        client
            .webClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder
                    .path("$path/$id")
                    .build()
            }.body(BodyInserters.fromValue(berichtUpdate))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
}