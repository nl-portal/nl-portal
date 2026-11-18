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
package nl.nlportal.berichten.graphql

import java.util.UUID
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.vtbapi.client.domain.VTBBericht
import nl.nlportal.vtbapi.client.domain.VTBBerichtType
import nl.nlportal.vtbapi.domain.VTBBerichtenPage
import nl.nlportal.vtbapi.service.VTBBerichtenService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class VTBBerichtenQuery(
    private val vtbBerichtenService: VTBBerichtenService,
) {
    @QueryMapping
    suspend fun getVTBBericht(
        authentication: CommonGroundAuthentication,
        @Argument id: UUID,
    ): VTBBericht? =
        vtbBerichtenService.getBericht(
            authentication = authentication,
            id = id,
        )

    @QueryMapping
    suspend fun getVTBBerichten(
        authentication: CommonGroundAuthentication,
        @Argument pageNumber: Int? = 1,
        @Argument pageSize: Int? = 20,
    ): VTBBerichtenPage =
        vtbBerichtenService.getBerichten(
            authentication = authentication,
            pageNumber = pageNumber ?: 1,
            pageSize = pageSize ?: 20,
        )

    @QueryMapping
    suspend fun getUnopenedVTBBerichtenCount(authentication: CommonGroundAuthentication): Int =
        vtbBerichtenService
            .getUnopenedBerichtenCount(
                authentication = authentication,
            )

    @SchemaMapping(typeName = "VTBBericht", field = "documenten")
    suspend fun documenten(
        bericht: VTBBericht,
    ): List<Document> {
        if (bericht.bijlagen.isEmpty()) {
            return emptyList()
        }
        return vtbBerichtenService.getDocumenten(
            identificatie = bericht.ontvanger,
            bijlages = bericht.bijlagen,
        )
    }

    @SchemaMapping(typeName = "VTBBericht", field = "berichtType")
    suspend fun berichtType(
        bericht: VTBBericht,
    ): VTBBerichtType =
        vtbBerichtenService.getBerichtType(
            url = bericht.berichtType,
        )
}