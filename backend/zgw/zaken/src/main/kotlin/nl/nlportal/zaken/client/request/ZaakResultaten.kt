package nl.nlportal.zaken.client.request

import nl.nlportal.zaken.domain.ZaakResultaat
import java.util.UUID

interface ZaakResultaten {
    fun get(id: UUID): GetZaakResultaat
}

interface GetZaakResultaat : Retrieve<ZaakResultaat>
