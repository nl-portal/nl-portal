package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.domain.ZaakRol
import java.util.UUID

interface ZaakRollen {
    fun search(): SearchZaakRollen

    fun get(id: UUID): GetZaakRol
}

interface SearchZaakRollen : PagedRetrieve<SearchZaakRollen, ZaakRol>, AuthenticationFilter<SearchZaakRollen> {
    fun forZaak(zaakUrl: String): SearchZaakRollen

    fun forZaak(zaakId: UUID): SearchZaakRollen

    fun ofVestigingsNummer(vestigingsNummer: String): SearchZaakRollen

    fun withOmschrijvingGeneriek(): SearchZaakRollen
}

interface GetZaakRol : Retrieve<ZaakRol>
