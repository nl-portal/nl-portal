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
package nl.nlportal.vtbapi.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.util.UUID
import kotlin.Any
import kotlin.Pair
import kotlinx.coroutines.flow.Flow
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.CoreUtils
import nl.nlportal.core.util.CoreUtils.extractId
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.vtbapi.client.VTBClient
import nl.nlportal.vtbapi.client.domain.ResultPage
import nl.nlportal.vtbapi.client.domain.VTBBericht
import nl.nlportal.vtbapi.client.domain.VTBBerichtFilters
import nl.nlportal.vtbapi.client.domain.VTBBerichtType
import nl.nlportal.vtbapi.client.domain.VTBBerichtUpdate
import nl.nlportal.vtbapi.client.domain.VTBBijlage
import nl.nlportal.vtbapi.client.path.VTBBerichtTypes
import nl.nlportal.vtbapi.client.path.VTBBerichten
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class VTBBerichtenService(
    private val vtbClient: VTBClient,
    private val documentenApiService: DocumentenApiService,
) {
    suspend fun getBerichten(
        authentication: CommonGroundAuthentication,
        pageNumber: Int,
        pageSize: Int,
        isOnGeopend: Boolean? = null,
    ): ResultPage<VTBBericht> {
        try {
            val searchVariables =
                mutableListOf<Pair<VTBBerichtFilters, Any>>(
                    VTBBerichtFilters.PAGE to pageNumber.toString(),
                    VTBBerichtFilters.PAGE_SIZE to pageSize.toString(),
                    VTBBerichtFilters.ONTVANGER to authentication.getUrn(),
                    VTBBerichtFilters.PUBLICATIEDATUM_LTE to LocalDateTime.now().toString(),
                )

            isOnGeopend?.let {
                searchVariables.add(VTBBerichtFilters.GEOPENDOP_NULL to isOnGeopend)
            }
            return vtbClient.path<VTBBerichten>().get(searchVariables)
        } catch (e: Exception) {
            logger.error { "Error getting berichten with cause: " + e.message }
        }

        return ResultPage(
            count = 0,
            results = emptyList(),
        )
    }

    suspend fun getUnopenedBerichtenCount(
        authentication: CommonGroundAuthentication,
    ): Int =
        getBerichten(
            authentication = authentication,
            pageNumber = 1,
            pageSize = 1,
            isOnGeopend = true,
        ).count

    suspend fun getBericht(
        authentication: CommonGroundAuthentication,
        id: UUID,
    ): VTBBericht? {
        val bericht =
            try {
                vtbClient.path<VTBBerichten>().get(id)
            } catch (e: Exception) {
                logger.error { "Error getting berichten with cause: " + e.message }
                null
            } ?: return null

        if (bericht.ontvanger != authentication.getUrn()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authorized for this Bericht")
        }

        // if bericht geopendOp is not null, direct return bericht
        if (bericht.geopendOp != null) {
            return bericht.apply { geopend = true }
        }

        return updateBericht(
            id = id,
        ).apply { geopend = geopendOp != null }
    }

    suspend fun updateBericht(id: UUID): VTBBericht =
        vtbClient.path<VTBBerichten>().patch(
            id = id,
            berichtUpdate =
                VTBBerichtUpdate(
                    geopendOp = LocalDateTime.now(),
                ),
        )

    suspend fun getBerichtType(url: String): VTBBerichtType =
        vtbClient.path<VTBBerichtTypes>().get(
            id = extractId(url),
        )

    suspend fun getDocumenten(
        identificatie: String,
        bijlages: List<VTBBijlage>,
    ): List<Document> =
        documentenApiService.filterDocuments(
            bijlages.map {
                documentenApiService
                    .getDocumentWithUrn(it.informatieObject)
                    .copy(identificatie = identificatie)
            },
        )

    suspend fun getBerichtDocumentContent(
        authentication: CommonGroundAuthentication,
        berichtId: UUID,
        documentId: UUID,
    ): Pair<Document, Flow<DataBuffer>>? {
        val vtbBericht =
            getBericht(
                authentication = authentication,
                id = berichtId,
            ) ?: return null

        if (vtbBericht.geopendOp == null) {
            logger.debug {
                "Access denied to document $documentId on bericht $berichtId for user ${authentication.userId}"
            }
            throw ResponseStatusException(HttpStatus.FORBIDDEN, ACCESS_DENIED)
        }

        val bijlage =
            vtbBericht.bijlagen.firstOrNull { documentenApiService.getDocumentUUIDFromURN(it.informatieObject) == documentId }
                ?: run {
                    logger.debug {
                        "Access denied to document $documentId on bericht $berichtId for user ${authentication.userId}"
                    }
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ACCESS_DENIED)
                }

        val (documentApi, documentId) =
            documentenApiService.getDocumentApiAndUUIDFromURN(
                documentUrn = bijlage.informatieObject,
            )

        val document =
            documentenApiService.getDocument(
                documentId = documentId,
                documentApi = documentApi,
            )
        val content =
            documentenApiService.getDocumentContentStreaming(
                documentId = documentId,
                documentApi = documentApi,
            )
        return document to content
    }

    companion object {
        val logger = KotlinLogging.logger {}
        const val ACCESS_DENIED = "Access denied for this Bericht"
    }
}