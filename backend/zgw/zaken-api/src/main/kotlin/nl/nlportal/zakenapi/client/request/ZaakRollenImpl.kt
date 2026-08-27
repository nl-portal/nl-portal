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
import nl.nlportal.zakenapi.domain.ZaakRol
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakRollenImpl(
    val zakenApiClient: ZakenApiClient,
) : ZaakRollen {
    override fun search(): SearchZaakRollen = SearchRollenImpl(zakenApiClient)

    override fun get(id: UUID): GetZaakRol = GetRolImpl(zakenApiClient, id)
}

class GetRolImpl(
    val zakenApiClient: ZakenApiClient,
    val id: UUID,
) : GetZaakRol {
    override suspend fun retrieve(): ZaakRol =
        this.zakenApiClient.webClient
            .get()
            .uri("/zaken/api/v1/rollen/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
}

class SearchRollenImpl(
    val zakenApiClient: ZakenApiClient,
) : SearchZaakRollen {
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()

    override fun withBsn(bsn: String): SearchZaakRollen {
        queryParams.add("betrokkeneIdentificatie__natuurlijkPersoon__inpBsn", bsn)
        return this
    }

    override fun withKvk(kvk: String): SearchZaakRollen {
        queryParams.add("betrokkeneIdentificatie__nietNatuurlijkPersoon__annIdentificatie", kvk)
        return this
    }

    override fun withUid(uid: String): SearchZaakRollen {
        queryParams.add("betrokkeneIdentificatie__natuurlijkPersoon__anpIdentificatie", uid)
        return this
    }

    override fun withKvkAndVestigingsNummer(
        kvkNummer: String,
        vestigingsNummer: String,
    ): SearchZaakRollen {
        queryParams.add("betrokkeneIdentificatie__vestiging__vestigingsNummer", vestigingsNummer)
        queryParams.add("betrokkeneIdentificatie__vestiging__kvkNummer", kvkNummer)
        return this
    }

    override fun ofVestigingsNummer(vestigingsNummer: String): SearchZaakRollen {
        queryParams.add("betrokkeneIdentificatie__vestiging__vestigingsNummer", vestigingsNummer)
        return this
    }

    override fun forZaak(zaakUrl: String): SearchZaakRollen {
        queryParams.add("zaak", zaakUrl)
        return this
    }

    override fun forZaak(zaakId: UUID): SearchZaakRollen {
        queryParams.add("zaak", this.zakenApiClient.getZaakUrl(zaakId))
        return this
    }

    override fun page(page: Int): SearchZaakRollen {
        queryParams.add("page", page.toString())
        return this
    }

    override fun pageSize(pageSize: Int): SearchZaakRollen {
        queryParams.add("pageSize", pageSize.toString())
        return this
    }

    override fun withOmschrijvingGeneriek(): SearchZaakRollen {
        queryParams.add("omschrijvingGeneriek", "initiator")
        return this
    }

    override suspend fun retrieve(): ResultPage<ZaakRol> =
        this.zakenApiClient.webClient
            .get()
            .uri { it.path("/zaken/api/v1/rollen").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
}