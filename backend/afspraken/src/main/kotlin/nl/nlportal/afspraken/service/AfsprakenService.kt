package nl.nlportal.afspraken.service

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nl.nlportal.afspraken.domain.AfspraakDetail
import nl.nlportal.afspraken.domain.AfspraakOverzicht
import nl.nlportal.afspraken.domain.AfsprakenResultaat
import nl.nlportal.afspraken.domain.IdentificatieInput
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

class AfsprakenService(
    private val webClient: WebClient,
) {
    suspend fun getAfspraken(
        identificaties: List<IdentificatieInput>,
        van: String?,
        tot: String?,
    ): AfsprakenResultaat {
        val requestBody = AfspraakOpvragenRequest(identificaties, van, tot)
        val response = webClient
            .post()
            .uri("/api/v1/afspraken/opvragen")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono<AfspraakOpvragenResponse>()
            .awaitSingleOrNull()

        return if (response != null) {
            AfsprakenResultaat(
                afspraken = response.afspraken,
                totalCount = response.totalCount,
            )
        } else {
            AfsprakenResultaat(afspraken = emptyList(), totalCount = 0)
        }
    }

    suspend fun getAfspraak(id: String): AfspraakDetail? {
        return try {
            webClient
                .get()
                .uri("/api/v1/afspraken/{id}", id)
                .retrieve()
                .onStatus({ it == HttpStatus.NOT_FOUND }, { reactor.core.publisher.Mono.empty() })
                .bodyToMono<AfspraakDetail>()
                .awaitSingleOrNull()
        } catch (e: Exception) {
            logger.warn { "Afspraak niet gevonden voor id=$id: ${e.message}" }
            null
        }
    }

    private data class AfspraakOpvragenRequest(
        val identificaties: List<IdentificatieInput>,
        val van: String?,
        val tot: String?,
    )

    private data class AfspraakOpvragenResponse(
        val afspraken: List<AfspraakOverzicht>,
        @JsonProperty("total_count") val totalCount: Int,
    )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
