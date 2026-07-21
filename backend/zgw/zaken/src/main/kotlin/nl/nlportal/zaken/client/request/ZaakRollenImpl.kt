package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.handleStatus
import nl.nlportal.zaken.domain.ResultPage
import nl.nlportal.zaken.domain.ZaakRol
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakRollenImpl(val zakenClient: ZakenClient) : ZaakRollen {
    override fun search(): SearchZaakRollen {
        return SearchRollenImpl(zakenClient)
    }

    override fun get(id: UUID): GetZaakRol {
        return GetRolImpl(zakenClient, id)
    }
}

class GetRolImpl(val zakenApiClient: ZakenClient, val id: UUID) : GetZaakRol {
    override suspend fun retrieve(): ZaakRol {
        return this.zakenApiClient.webClient.get()
            .uri("/zaken/api/v1/rollen/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}

class SearchRollenImpl(val zakenApiClient: ZakenClient) : SearchZaakRollen {
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

    override suspend fun retrieve(): ResultPage<ZaakRol> {
        return this.zakenApiClient.webClient.get()
            .uri { it.path("/zaken/api/v1/rollen").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}
