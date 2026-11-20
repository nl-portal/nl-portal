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
package nl.nlportal.core.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [SecurityTestApplication::class],
    properties = ["nl-portal.security.actuator.authorities-claim-expression=['resource_access']['nl-portal']['roles']"],
)
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(Lifecycle.PER_CLASS)
class ActuatorSecurityClientRoleTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(propsRegistry: DynamicPropertyRegistry) {
            propsRegistry.add("nl-portal.security.actuator.jwk-set-uri") { ActuatorJwks.jwkSetUri }
        }
    }

    @Test
    fun `refresh with the configured role in the client claim is allowed`() {
        webTestClient
            .post()
            .uri("/actuator/refresh")
            .headers {
                it.setBearerAuth(
                    ActuatorJwks.token(
                        mapOf("resource_access" to mapOf("nl-portal" to mapOf("roles" to listOf("ROLE_ACTUATOR")))),
                    ),
                )
            }.exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `refresh with the role only as a realm role is forbidden`() {
        webTestClient
            .post()
            .uri("/actuator/refresh")
            .headers { it.setBearerAuth(ActuatorJwks.token(ActuatorJwks.realmRoles("ROLE_ACTUATOR"))) }
            .exchange()
            .expectStatus()
            .isForbidden
    }
}