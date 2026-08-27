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
package nl.nlportal.zakenapi.client.request

import nl.nlportal.zakenapi.client.ZakenApiClient
import nl.nlportal.zakenapi.client.handleStatus
import nl.nlportal.zakenapi.domain.ZaakResultaat
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakResultatenImpl(
    val zakenApiClient: ZakenApiClient,
) : ZaakResultaten {
    override fun get(id: UUID): GetZaakResultaat = GetZaakResultatenImpl(zakenApiClient, id)
}

class GetZaakResultatenImpl(
    val zakenApiClient: ZakenApiClient,
    val id: UUID,
) : GetZaakResultaat {
    override suspend fun retrieve(): ZaakResultaat =
        this.zakenApiClient.webClient
            .get()
            .uri("/zaken/api/v1/resultaten/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
}