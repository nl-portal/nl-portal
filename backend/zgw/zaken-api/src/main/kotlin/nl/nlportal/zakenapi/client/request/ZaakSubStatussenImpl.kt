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
import nl.nlportal.zakenapi.domain.ResultPage
import nl.nlportal.zakenapi.domain.ZaakSubStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakSubStatussenImpl(
    val zakenApiClient: ZakenApiClient,
) : ZaakSubStatussen {
    override fun search(): SearchZaakSubStatussen = SearchZaakSubStatussenImpl(zakenApiClient)

    override fun get(id: UUID): GetZaakSubStatus = GetZaakSubStatussenImpl(zakenApiClient, id)
}

class GetZaakSubStatussenImpl(
    val zakenApiClient: ZakenApiClient,
    val id: UUID,
) : GetZaakSubStatus {
    override suspend fun retrieve(): ZaakSubStatus =
        this.zakenApiClient.webClient
            .get()
            .uri("/zaken/api/v1/substatussen/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
}

class SearchZaakSubStatussenImpl(
    val zakenApiClient: ZakenApiClient,
) : SearchZaakSubStatussen {
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()

    override fun forZaak(zaakUrl: String): SearchZaakSubStatussen {
        queryParams.add("zaak", zaakUrl)
        return this
    }

    override fun forStatus(zaakStatusUrl: String): SearchZaakSubStatussen {
        queryParams.add("status", zaakStatusUrl)
        return this
    }

    override fun page(page: Int): SearchZaakSubStatussen {
        queryParams.add("page", page.toString())
        return this
    }

    override fun pageSize(pageSize: Int): SearchZaakSubStatussen {
        queryParams.add("pageSize", pageSize.toString())
        return this
    }

    override suspend fun retrieve(): ResultPage<ZaakSubStatus> =
        this.zakenApiClient.webClient
            .get()
            .uri { it.path("/zaken/api/v1/substatussen").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
}