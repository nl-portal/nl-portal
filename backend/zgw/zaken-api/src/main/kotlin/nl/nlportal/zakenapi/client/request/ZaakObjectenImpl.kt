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
import nl.nlportal.zakenapi.domain.ZaakObject
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakObjectenImpl(
    val zakenApiClient: ZakenApiClient,
) : ZaakObjecten {
    override fun search(): SearchZaakObjecten = SearchZaakObjectenImpl(zakenApiClient)

    override fun get(id: UUID): GetZaakOject = GetZaakObjectImpl(zakenApiClient, id)
}

class SearchZaakObjectenImpl(
    val zakenApiClient: ZakenApiClient,
) : SearchZaakObjecten {
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()

    override fun forZaak(zaakUri: String): SearchZaakObjecten {
        queryParams.add("zaak", zaakUri)
        return this
    }

    override fun forZaak(id: UUID): SearchZaakObjecten {
        queryParams.add("zaak", zakenApiClient.getZaakUrl(id))
        return this
    }

    override fun ofObject(objectUri: String): SearchZaakObjecten {
        queryParams.add("object", objectUri)
        return this
    }

    override fun ofObjectType(objectType: ObjectType): SearchZaakObjecten {
        queryParams.add("objectType", objectType.value)
        return this
    }

    override fun page(page: Int): SearchZaakObjecten {
        queryParams.add("page", page.toString())
        return this
    }

    override fun pageSize(pageSize: Int): SearchZaakObjecten {
        queryParams.add("pageSize", pageSize.toString())
        return this
    }

    override suspend fun retrieve(): ResultPage<ZaakObject> =
        this.zakenApiClient.webClient
            .get()
            .uri { it.path("/zaken/api/v1/zaakobjecten").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
}

class GetZaakObjectImpl(
    val zakenApiClient: ZakenApiClient,
    val id: UUID,
) : GetZaakOject {
    override suspend fun retrieve(): ZaakObject =
        this.zakenApiClient.webClient
            .get()
            .uri { it.path("/zaken/api/v1/zaakobjecten/$id").build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
}