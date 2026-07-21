package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.domain.ZaakStatus
import java.util.UUID

interface ZaakStatussen {
    fun search(): SearchZaakStatussen

    fun get(id: UUID): GetZaakStatus
}

interface SearchZaakStatussen : PagedRetrieve<SearchZaakStatussen, ZaakStatus> {
    fun forZaak(zaakUrl: String): SearchZaakStatussen

    fun forZaak(zaakId: UUID): SearchZaakStatussen

    fun forStatustype(statustype: String): SearchZaakStatussen
}

interface GetZaakStatus : Retrieve<ZaakStatus>
