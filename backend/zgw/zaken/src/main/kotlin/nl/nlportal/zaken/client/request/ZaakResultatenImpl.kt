package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.client.ZakenClient
import nl.nlportal.zaken.client.handleStatus
import nl.nlportal.zaken.domain.ZaakResultaat
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

class ZaakResultatenImpl(val zakenApiClient: ZakenClient) : ZaakResultaten {
    override fun get(id: UUID): GetZaakResultaat {
        return GetZaakResultatenImpl(zakenApiClient, id)
    }
}

class GetZaakResultatenImpl(val zakenClient: ZakenClient, val id: UUID) : GetZaakResultaat {
    override suspend fun retrieve(): ZaakResultaat {
        return this.zakenClient.webClient.get()
            .uri("/zaken/api/v1/resultaten/$id")
            .retrieve()
            .handleStatus()
            .awaitBody()
    }
}
