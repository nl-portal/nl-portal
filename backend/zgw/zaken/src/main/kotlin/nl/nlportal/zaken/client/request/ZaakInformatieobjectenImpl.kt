package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.handleStatus
import nl.nlportal.zaken.domain.ZaakDocument
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZakenInformatieobjectenImpl(val zakenClient: ZakenClient) : ZaakInformatieobjecten {
    override fun search(): SearchZaakInformatieobjecten {
        return SearchZaakInformatieobjectenImpl(zakenClient)
    }

    override fun get(id: UUID): GetZaakInformatieobject {
        return GetZaakInformatieobjectImpl(zakenClient, id)
    }
}

class GetZaakInformatieobjectImpl(val zakenClient: ZakenClient, val id: UUID) : GetZaakInformatieobject {
    override suspend fun retrieve(): ZaakDocument {
        return this.zakenClient.webClient.get()
            .uri("/zaken/api/v1/zaakinformatieobjecten/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}

class SearchZaakInformatieobjectenImpl(val zakenClient: ZakenClient) : SearchZaakInformatieobjecten {
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()

    override fun forZaak(zaakUri: String): SearchZaakInformatieobjecten {
        queryParams.add("zaak", zaakUri)
        return this
    }

    override fun forZaak(id: UUID): SearchZaakInformatieobjecten {
        queryParams.add("zaak", zakenClient.getZaakUrl(id))
        return this
    }

    override fun ofInformatieobject(informatieobjectUri: String): SearchZaakInformatieobjecten {
        queryParams.add("informatieobject", informatieobjectUri)
        return this
    }

    override suspend fun retrieve(): List<ZaakDocument> {
        return this.zakenClient.webClient.get()
            .uri { it.path("/zaken/api/v1/zaakinformatieobjecten").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}
