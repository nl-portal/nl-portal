package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.handleStatus
import nl.nlportal.zaken.domain.ResultPage
import nl.nlportal.zaken.domain.ZaakSubStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakSubStatussenImpl(val zakenClient: ZakenClient) : ZaakSubStatussen {
    override fun search(): SearchZaakSubStatussen {
        return SearchZaakSubStatussenImpl(zakenClient)
    }

    override fun get(id: UUID): GetZaakSubStatus {
        return GetZaakSubStatussenImpl(zakenClient, id)
    }
}

class GetZaakSubStatussenImpl(val zakenClient: ZakenClient, val id: UUID) : GetZaakSubStatus {
    override suspend fun retrieve(): ZaakSubStatus {
        return this.zakenClient.webClient.get()
            .uri("/zaken/api/v1/substatussen/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}

class SearchZaakSubStatussenImpl(val zakenClient: ZakenClient) : SearchZaakSubStatussen {
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

    override suspend fun retrieve(): ResultPage<ZaakSubStatus> {
        return this.zakenClient.webClient.get()
            .uri { it.path("/zaken/api/v1/substatussen").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}
