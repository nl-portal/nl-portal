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
package nl.nlportal.zakenapi.client.request

import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.commonground.authentication.KeycloakUserAuthentication

@Suppress("UNCHECKED_CAST")
interface AuthenticationFilter<T : AuthenticationFilter<T>> {
    fun withBsn(bsn: String): T

    fun withKvk(kvk: String): T

    fun withUid(uid: String): T

    fun withKvkAndVestigingsNummer(
        kvkNummer: String,
        vestigingsNummer: String,
    ): T

    fun withAuthentication(authentication: CommonGroundAuthentication): T {
        when (authentication) {
            is BurgerAuthentication -> {
                this.withBsn(authentication.userId)
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    this.withKvkAndVestigingsNummer(authentication.userId, vestigingsNummer)
                } else {
                    this.withKvk(authentication.userId)
                }
                //
            }

            is KeycloakUserAuthentication -> {
                this.withUid(authentication.userId)
            }

            else -> {
                throw IllegalArgumentException("Cannot get zaken for this user")
            }
        }

        return this as T
    }
}