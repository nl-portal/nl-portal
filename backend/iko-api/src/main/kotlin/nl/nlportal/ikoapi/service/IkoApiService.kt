/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.ikoapi.service

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.Mapper
import nl.nlportal.ikoapi.client.IkoApiClient
import nl.nlportal.ikoapi.client.IkoApiConfig.IkoApiConfigProperties
import nl.nlportal.ikoapi.domain.IkoContainerParam
import nl.nlportal.ikoapi.domain.IkoErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.JsonNode
import java.util.Base64

/**
 * Reads IKO aggregated data profiles for the logged-in burger (BSN) or bedrijf (KVK).
 *
 * The id sent to IKO always comes from [CommonGroundAuthentication], never from caller input.
 */
class IkoApiService(
    private val ikoApiClient: IkoApiClient,
    private val properties: IkoApiConfigProperties,
) {
    /**
     * @return the profile JSON as returned by IKO, or `null` when the user has no BSN or KVK or IKO answers 404
     * @throws IkoApiException when [name] is not in `allowed-profiles` or IKO fails; the message carries IKO's correlationId, never IKO's message
     */
    suspend fun getAggregatedDataProfile(
        authentication: CommonGroundAuthentication,
        name: String,
        containerParams: List<IkoContainerParam> = emptyList(),
    ): JsonNode? {
        if (name !in properties.allowedProfiles) {
            throw IkoApiException("IKO profile '$name' is not available")
        }
        val id =
            when (authentication) {
                is BurgerAuthentication -> {
                    authentication.getBsn()
                }

                is BedrijfAuthentication -> {
                    authentication.getKvkNummer()
                }

                else -> {
                    logger.warn { "IKO profiles are only available for burger and bedrijf users" }
                    return null
                }
            }
        return try {
            ikoApiClient.getAggregatedDataProfile(name, id, containerParams.map(::encode))
        } catch (ex: WebClientResponseException) {
            val error = runCatching { Mapper.get().readValue(ex.responseBodyAsByteArray, IkoErrorResponse::class.java) }.getOrNull()
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn { "IKO profile '$name' not found (correlationId=${error?.correlationId})" }
                return null
            }
            logger.error(ex) { "IKO profile '$name' returned ${ex.statusCode}: ${error?.message} (correlationId=${error?.correlationId})" }
            throw IkoApiException("IKO request for profile '$name' failed (correlationId=${error?.correlationId})", ex)
        } catch (ex: IkoApiException) {
            throw ex
        } catch (ex: Exception) {
            throw IkoApiException("IKO request for profile '$name' failed", ex)
        }
    }

    private fun encode(containerParam: IkoContainerParam): String = Base64.getEncoder().encodeToString(Mapper.get().writeValueAsBytes(containerParam.toPayload()))

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}