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
package nl.nlportal.documentenapi.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import nl.nlportal.commonground.authentication.URN_HR_PREFIX
import nl.nlportal.core.util.CoreUtils.extractId
import nl.nlportal.documentenapi.client.DocumentApisConfig.DocumentenApisConfigProperties
import nl.nlportal.documentenapi.client.DocumentenApiClient
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.documentenapi.domain.DocumentStatus
import nl.nlportal.documentenapi.domain.PostEnkelvoudiginformatieobjectRequest
import nl.nlportal.documentenapi.exceptions.MimeTypeDeniedException
import nl.nlportal.portal.authentication.domain.PortalAuthentication
import org.apache.tika.Tika
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.security.core.context.ReactiveSecurityContextHolder

class DocumentenApiService(
    val documentenApiClient: DocumentenApiClient,
    val documentenApisConfigProperties: DocumentenApisConfigProperties,
) {
    suspend fun getDocument(
        documentId: UUID,
        documentApi: String,
    ): Document = documentenApiClient.getDocument(documentId, documentApi)

    suspend fun getDocument(documentUrl: String): Document =
        documentenApiClient.getDocument(
            extractId(documentUrl),
            documentenApisConfigProperties.getConfigForDocumentUrl(documentUrl),
        )

    suspend fun getDocumentWithUrn(documentUrn: String): Document {
        val (documentApi, uuid) =
            getDocumentApiAndUUIDFromURN(
                documentUrn = documentUrn,
            )
        return documentenApiClient.getDocument(
            id = uuid,
            documentApi = documentApi,
        )
    }

    fun getDocumentContentStreaming(
        documentId: UUID,
        documentApi: String,
    ): Flow<DataBuffer> = documentenApiClient.getDocumentContentStream(documentId, documentApi)

    fun getDocumentContentStreaming(informatieobejctUrl: String): Flow<DataBuffer> {
        val informatieObjectId = extractId(informatieobejctUrl)
        val documentenApi = documentenApisConfigProperties.getConfigForDocumentUrl(informatieobejctUrl)

        return documentenApiClient.getDocumentContentStream(informatieObjectId, documentenApi)
    }

    suspend fun uploadDocument(
        content: ByteArray,
        filename: String,
        documentApi: String,
        informatieobjecttype: String? = null,
        beschrijving: String? = null,
    ): Document {
        val auteur =
            ReactiveSecurityContextHolder
                .getContext()
                .map { (it.authentication as PortalAuthentication).userId }
                .awaitSingleOrNull() ?: "valtimo"
        val documentenApiConfig = documentenApisConfigProperties.getConfig(documentApi)

        if (documentenApisConfigProperties.allowedMimeTypes.isNotEmpty()) {
            val mediaType = Tika().detect(content).split(";")[0].trim()
            if (mediaType !in documentenApisConfigProperties.allowedMimeTypes) {
                throw MimeTypeDeniedException("$mediaType is not allowed for uploads.")
            }
        }

        return documentenApiClient.postDocument(
            PostEnkelvoudiginformatieobjectRequest(
                bronorganisatie = documentenApiConfig.rsin!!,
                creatiedatum = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                titel = filename,
                auteur = auteur,
                status = DocumentStatus.DEFINITIEF,
                taal = "nld",
                bestandsnaam = filename,
                indicatieGebruiksrecht = false,
                informatieobjecttype =
                    informatieobjecttype
                        .takeUnless {
                            it.isNullOrEmpty()
                        }
                        ?: documentenApiConfig.documentTypeUrl!!,
                beschrijving = beschrijving.takeUnless { it.isNullOrBlank() },
            ),
            content,
            documentApi,
        )
    }

    fun filterDocuments(
        documents: List<Document>,
    ): List<Document> =
        documents.filter { document ->
            document.status in documentenApisConfigProperties.statusWhitelist &&
                document.vertrouwelijkheidaanduiding in documentenApisConfigProperties.vertrouwelijkheidsaanduidingWhitelist
        }

    fun getDocumentApiAndUUIDFromURN(
        documentUrn: String,
    ): Pair<String, UUID> {
        val splitURN = documentUrn.split(":")
        require(splitURN.size == 8) { "Invalid document Urn format." }

        val documentApi = splitURN[3]
        val uuid = splitURN[7]

        // to check if document is configured
        documentenApisConfigProperties.getConfig(documentApi)
        return documentApi to UUID.fromString(uuid)
    }

    fun getDocumentUUIDFromURN(
        documentUrn: String,
    ): UUID = getDocumentApiAndUUIDFromURN(documentUrn).second
}