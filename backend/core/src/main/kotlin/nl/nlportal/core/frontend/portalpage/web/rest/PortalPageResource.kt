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
package nl.nlportal.core.frontend.portalpage.web.rest

import nl.nlportal.core.frontend.portalpage.PortalPageNotFoundException
import nl.nlportal.core.frontend.portalpage.PortalPageOverrideException
import nl.nlportal.core.frontend.portalpage.PortalPageService
import nl.nlportal.core.frontend.portalpage.domain.PortalPageBindingRefetchRequest
import nl.nlportal.core.frontend.portalpage.domain.PortalPageBindingResponse
import nl.nlportal.core.frontend.portalpage.domain.PortalPageResponse
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.util.Locale

@RestController
@RequestMapping(value = ["/api/frontend/portal-pages"])
class PortalPageResource(
    private val portalPageService: PortalPageService,
) {
    @GetMapping("/{page}")
    suspend fun page(
        @PathVariable page: String,
        authentication: Authentication,
        exchange: ServerWebExchange,
    ): PortalPageResponse = portalPageService.getPage(context(authentication, exchange), page)

    @GetMapping("/{page}/{id}")
    suspend fun pageForId(
        @PathVariable page: String,
        @PathVariable id: String,
        authentication: Authentication,
        exchange: ServerWebExchange,
    ): PortalPageResponse = portalPageService.getPage(context(authentication, exchange), page, id)

    @PostMapping("/{page}/bindings/{alias}")
    suspend fun refetch(
        @PathVariable page: String,
        @PathVariable alias: String,
        @RequestBody request: PortalPageBindingRefetchRequest,
        authentication: Authentication,
        exchange: ServerWebExchange,
    ): PortalPageBindingResponse = portalPageService.refetch(context(authentication, exchange), page, alias, request.params)

    @ExceptionHandler(PortalPageNotFoundException::class)
    fun notFound(): ResponseEntity<Map<String, String>> = ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Page not found"))

    @ExceptionHandler(PortalPageOverrideException::class)
    fun badRequest(): ResponseEntity<Map<String, String>> = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to "Invalid params"))

    private fun context(
        authentication: Authentication,
        exchange: ServerWebExchange,
    ): PortalPageRequestContext {
        val locale =
            runCatching {
                exchange.request.headers.acceptLanguageAsLocales
                    .firstOrNull()
            }.getOrNull()
                ?.takeIf { it.language.isNotEmpty() }
                ?: DEFAULT_LOCALE
        return PortalPageRequestContext(authentication, locale)
    }

    companion object {
        private val DEFAULT_LOCALE: Locale = Locale.forLanguageTag("nl")
    }
}