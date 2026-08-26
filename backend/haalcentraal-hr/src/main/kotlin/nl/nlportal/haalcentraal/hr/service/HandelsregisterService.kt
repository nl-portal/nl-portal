/*
 * Copyright 2015-2023 Ritense BV, the Netherlands.
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
package nl.nlportal.haalcentraal.hr.service

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.haalcentraal.hr.client.HandelsregisterClient
import nl.nlportal.haalcentraal.hr.domain.MaatschappelijkeActiviteit

class HandelsregisterService(
    private val handelsregisterClient: HandelsregisterClient,
) {
    suspend fun getMaatschappelijkeActiviteit(authentication: CommonGroundAuthentication): MaatschappelijkeActiviteit? {
        if (authentication is BedrijfAuthentication) {
            return getKvkData(
                kvkNummer = authentication.getKvkNummer(),
                vestigingsNummer = authentication.getVestigingsNummer(),
            )
        }

        return null
    }

    suspend fun getGemachtigde(authentication: CommonGroundAuthentication): MaatschappelijkeActiviteit? {
        val authenticationGemachtigde = authentication.getGemachtigde()

        return authenticationGemachtigde?.kvk?.let {
            getKvkData(
                kvkNummer = it,
                vestigingsNummer = null,
            )
        }
    }

    suspend fun getKvkData(
        kvkNummer: String,
        vestigingsNummer: String? = null,
    ): MaatschappelijkeActiviteit? {
        try {
            val kvkData = handelsregisterClient.getMaatschappelijkeActiviteit(kvkNummer)

            // set hoofdvesting in vestiging
            kvkData.embedded?.vestiging = kvkData.embedded.hoofdvestiging

            if (!vestigingsNummer.isNullOrBlank()) {
                // set vestiging based of vestigingnummer in vestiging
                kvkData.embedded?.vestiging = handelsregisterClient.getVestiging(vestigingsNummer)
            }
            return kvkData
        } catch (ex: Exception) {
            logger.error { "Something went wrong while getting information of a company: ${ex.message}" }
        }

        return null
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}