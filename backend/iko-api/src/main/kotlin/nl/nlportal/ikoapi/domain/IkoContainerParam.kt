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
package nl.nlportal.ikoapi.domain

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * Paging, sorting and filtering for one container of an aggregated data profile: the profile itself or one of its relations.
 */
data class IkoContainerParam(
    val containerId: String,
    val pageable: Pageable = Pageable.unpaged(),
    val filters: Map<String, String> = emptyMap(),
) {
    fun toPayload(): IkoContainerParamPayload =
        IkoContainerParamPayload(
            containerId = containerId,
            pageable =
                pageable.takeIf { it.isPaged }?.let { paged ->
                    IkoPageablePayload(
                        pageNumber = paged.pageNumber,
                        pageSize = paged.pageSize,
                        sort =
                            paged.sort
                                .map(IkoSortOrderPayload::from)
                                .toList()
                                .ifEmpty { null },
                    )
                },
            filters = filters.ifEmpty { null },
        )
}

data class IkoContainerParamPayload(
    val containerId: String,
    val pageable: IkoPageablePayload? = null,
    val filters: Map<String, String>? = null,
)

data class IkoPageablePayload(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: List<IkoSortOrderPayload>? = null,
)

data class IkoSortOrderPayload(
    val property: String,
    val direction: Sort.Direction,
    val ignoreCase: Boolean? = null,
    val nullHandling: Sort.NullHandling? = null,
) {
    companion object {
        fun from(order: Sort.Order): IkoSortOrderPayload =
            IkoSortOrderPayload(
                property = order.property,
                direction = order.direction,
                ignoreCase = order.isIgnoreCase.takeIf { it },
                nullHandling = order.nullHandling.takeIf { it != Sort.NullHandling.NATIVE },
            )
    }
}