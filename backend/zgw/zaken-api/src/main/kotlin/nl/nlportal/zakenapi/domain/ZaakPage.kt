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
package nl.nlportal.zakenapi.domain

import nl.nlportal.graphql.Page
import nl.nlportal.zakenapi.domain.ResultPage
import nl.nlportal.zakenapi.domain.Zaak

class ZaakPage(
    number: Int,
    size: Int,
    content: List<Zaak>,
    totalElements: Int,
) : Page<Zaak>(number, size, content, totalElements) {
    companion object {
        fun fromResultPage(
            pageNumber: Int,
            pageSize: Int,
            resultPage: ResultPage<Zaak>,
        ): ZaakPage {
            val zaken = resultPage.results.map { it }

            return ZaakPage(
                number = pageNumber,
                size = pageSize,
                content = zaken,
                totalElements = resultPage.count,
            )
        }
    }
}