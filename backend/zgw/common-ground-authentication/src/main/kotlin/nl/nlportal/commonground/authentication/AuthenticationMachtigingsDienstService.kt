/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.commonground.authentication

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import nl.nlportal.core.util.Mapper
import org.springframework.core.io.ResourceLoader
import tools.jackson.core.type.TypeReference

class AuthenticationMachtigingsDienstService(
    val authenticationMachtingsDienstConfig: AuthenticationMachtigingsDienstConfig,
    resourceLoader: ResourceLoader,
) {
    var authenticationMachtingDiensten: MutableList<AuthenticationMachtigingsDienst> = mutableListOf()

    init {
        try {
            // first load the new machtigingsdienst configurations from the configurations properties
            authenticationMachtingDiensten.addAll(authenticationMachtingsDienstConfig.getMachtigingsDienstListFromConfiguration())

            // second load the machtigingsdienst from a resource url, to make it backwards compatible
            if (authenticationMachtingsDienstConfig.resourceUrl != null) {
                val json = resourceLoader.getResource(authenticationMachtingsDienstConfig.resourceUrl!!).getContentAsString(Charsets.UTF_8)
                logger.debug { "Machtigingsdiensten is loaded: $json" }
                this.authenticationMachtingDiensten.addAll(Mapper.get().readValue(json, object : TypeReference<List<AuthenticationMachtigingsDienst>>() {}))
            }
        } catch (ex: Exception) {
            logger.warn { "Could not load json from ${authenticationMachtingsDienstConfig.resourceUrl} with reason ${ex.message}" }
        }
    }

    fun getAuthenticationMachtingDienst(uuid: UUID?): AuthenticationMachtigingsDienst? {
        if (uuid == null) return null
        return authenticationMachtingDiensten.find { it.uuid == uuid }
    }

    fun hasMachtingDienst(authentication: CommonGroundAuthentication): Boolean = authentication.machtigingsDienstUUIDs(authenticationMachtingsDienstConfig.allMachtigingUuid) != null

    fun zaakTypes(authentication: CommonGroundAuthentication): List<UUID>? {
        if (authentication !is BedrijfAuthentication) {
            return null
        }
        val zaakTypeList = mutableListOf<UUID>()
        authentication.machtigingsDienstUUIDs(authenticationMachtingsDienstConfig.allMachtigingUuid)?.forEach {
            getAuthenticationMachtingDienst(it)?.let {
                zaakTypeList.addAll(it.zaakTypes)
            }
        }

        return when {
            zaakTypeList.isEmpty() -> {
                null
            }

            else -> {
                zaakTypeList
            }
        }
    }

    fun taakTypes(authentication: CommonGroundAuthentication): List<String>? {
        if (authentication !is BedrijfAuthentication) {
            return null
        }
        val taakTypeList = mutableListOf<String>()
        authentication.machtigingsDienstUUIDs(authenticationMachtingsDienstConfig.allMachtigingUuid)?.forEach {
            getAuthenticationMachtingDienst(it)?.let {
                taakTypeList.addAll(it.taakTypes)
            }
        }
        return when {
            taakTypeList.isEmpty() -> {
                null
            }

            else -> {
                taakTypeList
            }
        }
    }

    fun productTypes(authentication: CommonGroundAuthentication): List<UUID>? {
        if (authentication !is BedrijfAuthentication) {
            return null
        }
        val productTypeList = mutableListOf<UUID>()
        authentication.machtigingsDienstUUIDs(authenticationMachtingsDienstConfig.allMachtigingUuid)?.forEach {
            getAuthenticationMachtingDienst(it)?.let {
                productTypeList.addAll(it.productTypes)
            }
        }
        return when {
            productTypeList.isEmpty() -> {
                null
            }

            else -> {
                productTypeList
            }
        }
    }

    fun isAllowedZaakType(
        authentication: CommonGroundAuthentication,
        zaakTypeUUID: UUID,
    ): Boolean {
        if (authentication !is BedrijfAuthentication) {
            return true
        }
        val zaaktypes = zaakTypes(authentication)

        if (!zaaktypes.isNullOrEmpty()) {
            return zaaktypes.contains(zaakTypeUUID)
        }

        return true
    }

    fun isAllowedZaakTypes(
        authentication: CommonGroundAuthentication,
        zaakTypeUUIDs: List<UUID>,
    ): Boolean {
        val allowedZaakTypes =
            filterAllowedZaakTypes(
                authentication = authentication,
                zaakTypeUUIDs = zaakTypeUUIDs,
            )
        if (allowedZaakTypes.isEmpty()) {
            return false
        }

        return true
    }

    fun filterAllowedZaakTypes(
        authentication: CommonGroundAuthentication,
        zaakTypeUUIDs: List<UUID>,
    ): List<UUID> {
        val zaaktypes = zaakTypes(authentication)
        if (authentication !is BedrijfAuthentication || zaaktypes.isNullOrEmpty()) {
            return zaakTypeUUIDs
        }

        return zaakTypeUUIDs.filter { it in zaaktypes }
    }

    fun isAllowedTaakType(
        authentication: CommonGroundAuthentication,
        taakType: String,
    ): Boolean {
        if (authentication !is BedrijfAuthentication) {
            return true
        }
        val taaktypes = taakTypes(authentication)

        if (!taaktypes.isNullOrEmpty()) {
            return taaktypes.contains(taakType)
        }

        return true
    }

    fun isAllowedProductType(
        authentication: CommonGroundAuthentication,
        productTypeUUID: UUID,
    ): Boolean {
        if (authentication !is BedrijfAuthentication) {
            return true
        }
        val productTypes = productTypes(authentication)

        if (!productTypes.isNullOrEmpty()) {
            return productTypes.contains(productTypeUUID)
        }

        return true
    }

    fun isAllowedProductTypes(
        authentication: CommonGroundAuthentication,
        productTypeUUIDs: List<UUID>,
    ): Boolean {
        if (authentication !is BedrijfAuthentication) {
            return true
        }
        val productTypes = productTypes(authentication)

        val allowedProductTypes = mutableSetOf<UUID>()

        if (!productTypes.isNullOrEmpty()) {
            productTypeUUIDs.forEach {
                if (productTypes.contains(it)) {
                    allowedProductTypes.add(it)
                }
            }

            return allowedProductTypes.isNotEmpty()
        }

        return true
    }

    companion object {
        private val logger: KLogger = KotlinLogging.logger {}
    }
}