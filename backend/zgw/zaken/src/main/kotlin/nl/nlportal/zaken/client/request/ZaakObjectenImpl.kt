package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.handleStatus
import nl.nlportal.zaken.domain.ResultPage
import nl.nlportal.zaken.domain.ZaakObject
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakObjectenImpl(val zakenClient: ZakenClient) : ZaakObjecten {
    override fun search(): SearchZaakObjecten {
        return SearchZaakObjectenImpl(zakenClient)
    }

    override fun get(id: UUID): GetZaakOject {
        return GetZaakObjectImpl(zakenClient, id)
    }
}

class SearchZaakObjectenImpl(val zakenClient: ZakenClient) : SearchZaakObjecten {
    val queryParams: MultiValueMap<String, String> = LinkedMultiValueMap()

    override fun forZaak(zaakUri: String): SearchZaakObjecten {
        queryParams.add("zaak", zaakUri)
        return this
    }

    override fun forZaak(id: UUID): SearchZaakObjecten {
        queryParams.add("zaak", zakenClient.getZaakUrl(id))
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

    override suspend fun retrieve(): ResultPage<ZaakObject> {
        return this.zakenClient.webClient.get()
            .uri { it.path("/zaken/api/v1/zaakobjecten").queryParams(queryParams).build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}

class GetZaakObjectImpl(val zakenApiClient: ZakenClient, val id: UUID) : GetZaakOject {
    override suspend fun retrieve(): ZaakObject {
        return this.zakenApiClient.webClient.get()
            .uri { it.path("/zaken/api/v1/zaakobjecten/$id").build() }
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}
