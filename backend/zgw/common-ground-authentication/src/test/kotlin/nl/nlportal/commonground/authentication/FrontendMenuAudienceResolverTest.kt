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
package nl.nlportal.commonground.authentication

import nl.nlportal.core.frontend.menu.FrontendMenuAudienceResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FrontendMenuAudienceResolverTest {
    private val resolver = AuthenticationConfiguration().commonGroundFrontendMenuAudienceResolver()

    @Test
    fun `burger resolves to burger`() {
        val jwt = JwtBuilder().aanvragerBsn("1234").buildJwt()

        assertEquals("burger", resolver.resolve(BurgerAuthentication(jwt, emptyList())))
    }

    @Test
    fun `bedrijf resolves to bedrijf`() {
        val jwt = JwtBuilder().aanvragerKvk("1234").buildJwt()

        assertEquals("bedrijf", resolver.resolve(BedrijfAuthentication(jwt, emptyList())))
    }

    @Test
    fun `keycloak user resolves to subject`() {
        val jwt = JwtBuilder().aanvragerUid(UUID.randomUUID()).buildJwt()

        assertEquals(FrontendMenuAudienceResolver.SUBJECT, resolver.resolve(KeycloakUserAuthentication(jwt, emptyList())))
    }
}