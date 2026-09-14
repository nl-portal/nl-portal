package nl.nlportal.afspraken.graphql

import nl.nlportal.afspraken.domain.AfspraakDetail
import nl.nlportal.afspraken.domain.AfsprakenResultaat
import nl.nlportal.afspraken.domain.IdentificatieInput
import nl.nlportal.afspraken.service.AfsprakenService
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AfsprakenQuery(
    private val afsprakenService: AfsprakenService,
) {
    @QueryMapping
    suspend fun getAfspraken(
        authentication: CommonGroundAuthentication,
        @Argument identificaties: List<IdentificatieInput>,
        @Argument van: String?,
        @Argument tot: String?,
    ): AfsprakenResultaat =
        afsprakenService.getAfspraken(
            identificaties = identificaties,
            van = van,
            tot = tot,
        )

    @QueryMapping
    suspend fun getAfspraak(
        authentication: CommonGroundAuthentication,
        @Argument id: String,
    ): AfspraakDetail? =
        afsprakenService.getAfspraak(id = id)
}
