package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.domain.ZaakSubStatus
import java.util.UUID

interface ZaakSubStatussen {
    fun search(): SearchZaakSubStatussen

    fun get(id: UUID): GetZaakSubStatus
}

interface SearchZaakSubStatussen : PagedRetrieve<SearchZaakSubStatussen, ZaakSubStatus> {
    fun forZaak(zaakUrl: String): SearchZaakSubStatussen

    fun forStatus(zaakStatusUrl: String): SearchZaakSubStatussen
}

interface GetZaakSubStatus : Retrieve<ZaakSubStatus>
