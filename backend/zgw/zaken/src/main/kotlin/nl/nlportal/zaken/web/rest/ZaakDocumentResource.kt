/*
 * Copyright 2015-2024 Ritense BV, the Netherlands.
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
package nl.nlportal.zaken.web.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.documentenapi.util.FilenameSanitizer
import nl.nlportal.zakenapi.service.ZakenApiService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(value = ["/api"])
class ZaakDocumentResource(
    private val zakenApiService: ZakenApiService,
) {
    @GetMapping(value = ["/zakenapi/zaakdocument/{zaakDocumentId}/content"])
    suspend fun getContentStreaming(
        @PathVariable zaakDocumentId: String,
        authentication: CommonGroundAuthentication,
    ): ResponseEntity<Flow<DataBuffer>> {
        val (document, content) =
            zakenApiService
                .getZaakDocumentContent(
                    zaakDocumentId = zaakDocumentId,
                    commonGroundAuthentication = authentication,
                )

        if (document == null || content == null) {
            logger.debug {
                "Access denied to document $zaakDocumentId for user ${authentication.userId}"
            }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied")
        }

        return ResponseEntity.ok()
            .headers(
                HttpHeaders().apply {
                    set(
                        "Content-Disposition",
                        FilenameSanitizer.encodeForContentDisposition(document.bestandsnaam),
                    )
                },
            )
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
