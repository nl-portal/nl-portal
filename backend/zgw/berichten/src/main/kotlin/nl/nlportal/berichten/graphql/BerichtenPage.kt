/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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

import nl.nlportal.berichten.domain.Bericht
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.graphql.Page
import nl.nlportal.vtbapi.client.domain.VTBBericht
import nl.nlportal.zgw.objectenapi.domain.ObjectsApiObject
import nl.nlportal.zgw.objectenapi.domain.ResultPage

class BerichtenPage(
    number: Int,
    size: Int,
    content: List<Bericht>,
    totalElements: Int,
) : Page<Bericht>(
        number,
        size,
        content,
        totalElements,
    ) {
    companion object {
        fun fromResultPage(
            pageNumber: Int,
            pageSize: Int,
            resultPage: ResultPage<ObjectsApiObject<Bericht>>,
        ): BerichtenPage {
            val berichten = resultPage.results.map { it.record.data.copy(id = it.uuid) }.sortedByDescending { it.publicatiedatum }

            return BerichtenPage(
                number = pageNumber,
                size = pageSize,
                content = berichten,
                totalElements = resultPage.count,
            )
        }
    }
}