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
package nl.nlportal.ikoapi.client

import nl.nlportal.ikoapi.service.IkoApiException
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Attaches a client-credentials access token from IKO's Keycloak realm to every request.
 *
 * The principal is fixed to the client id, so all users share one cached token; token reuse and
 * refresh (60 s before expiry) come from the client-credentials authorized-client provider.
 * The OAuth2 client objects are private to this filter, not Spring beans, so Boot's and
 * nl-portal's security auto-configuration stay untouched.
 */
class IkoClientCredentialsFilter(
    tokenUrl: URI,
    private val clientId: String,
    clientSecret: String,
) : ExchangeFilterFunction {
    private val authorizedClientManager: ReactiveOAuth2AuthorizedClientManager =
        run {
            val registration =
                ClientRegistration
                    .withRegistrationId(REGISTRATION_ID)
                    .tokenUri(tokenUrl.toString())
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .build()
            val registrations = InMemoryReactiveClientRegistrationRepository(registration)
            AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                registrations,
                InMemoryReactiveOAuth2AuthorizedClientService(registrations),
            ).apply {
                setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
            }
        }

    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> =
        authorizedClientManager
            .authorize(OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID).principal(clientId).build())
            .switchIfEmpty(Mono.error { IkoApiException("Could not obtain IKO access token") })
            .map { client -> ClientRequest.from(request).headers { it.setBearerAuth(client.accessToken.tokenValue) }.build() }
            .flatMap(next::exchange)

    companion object {
        const val REGISTRATION_ID = "iko"
    }
}