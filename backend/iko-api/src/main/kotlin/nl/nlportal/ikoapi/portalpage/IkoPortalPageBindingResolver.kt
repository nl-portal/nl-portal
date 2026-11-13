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
package nl.nlportal.ikoapi.portalpage

import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingData
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.ikoapi.domain.IkoContainerParam
import nl.nlportal.ikoapi.service.IkoApiService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import tools.jackson.databind.node.JsonNodeFactory

/**
 * Binding data from an IKO aggregated data profile of the logged-in subject, for dynamic portal pages.
 * Paging, sorting and filters apply to one container of the profile.
 */
class IkoPortalPageBindingResolver(
    private val ikoApiService: IkoApiService,
) : PortalPageBindingResolver {
    override val type = "iko"

    override val fixedParams = setOf(PROFILE, CONTAINER)

    override fun validate(params: PortalPageParams): List<String> =
        buildList {
            if (params.string(PROFILE) == null) add("$PROFILE is required")
            if (params.string(CONTAINER) == null && usesContainer(params)) {
                add("$PAGE, $SIZE, $SORT and $FILTER.* require a $CONTAINER")
            }
            checkInt(params, PAGE, "$PAGE must be >= 0") { it >= 0 }
            checkInt(params, SIZE, "$SIZE must be between 1 and $MAX_SIZE") { it in 1..MAX_SIZE }
            params.list(SORT).filterNot { SORT_ITEM.matches(it) }.forEach { add("$SORT item '$it' must be field:asc or field:desc") }
        }

    override suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
    ): PortalPageBindingData {
        val authentication =
            context.authentication as? CommonGroundAuthentication
                ?: throw IllegalStateException("IKO pages require a CommonGroundAuthentication")

        val containerParams =
            params.string(CONTAINER)?.let { container ->
                listOf(
                    IkoContainerParam(
                        containerId = container,
                        pageable = PageRequest.of(params.int(PAGE) ?: DEFAULT_PAGE, params.int(SIZE) ?: DEFAULT_SIZE, sort(params)),
                        filters = params.withPrefix(FILTER),
                    ),
                )
            } ?: emptyList()

        val data =
            ikoApiService.getAggregatedDataProfile(authentication, params.string(PROFILE)!!, containerParams)
                ?: JsonNodeFactory.instance.objectNode()
        return PortalPageBindingData(data = data)
    }

    private fun usesContainer(params: PortalPageParams): Boolean =
        listOf(PAGE, SIZE).any { params.string(it) != null } ||
            params.list(SORT).isNotEmpty() ||
            params.withPrefix(FILTER).isNotEmpty()

    private fun sort(params: PortalPageParams): Sort =
        Sort.by(
            params.list(SORT).map {
                val (property, direction) = it.split(":")
                Sort.Order(Sort.Direction.fromString(direction), property)
            },
        )

    private fun MutableList<String>.checkInt(
        params: PortalPageParams,
        key: String,
        problem: String,
        valid: (Int) -> Boolean,
    ) {
        val value =
            try {
                params.int(key)
            } catch (e: IllegalArgumentException) {
                add("$key must be a number")
                return
            }
        if (value != null && !valid(value)) add(problem)
    }

    companion object {
        const val PROFILE = "profile"
        const val CONTAINER = "container"
        const val PAGE = "page"
        const val SIZE = "size"
        const val SORT = "sort"
        const val FILTER = "filter"
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SIZE = 10
        private const val MAX_SIZE = 100
        private val SORT_ITEM = Regex("^[A-Za-z0-9_.]+:(?i)(asc|desc)$")
    }
}