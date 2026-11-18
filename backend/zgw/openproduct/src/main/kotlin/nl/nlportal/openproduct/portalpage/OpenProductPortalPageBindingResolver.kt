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
package nl.nlportal.openproduct.portalpage

import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingData
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.util.Mapper
import nl.nlportal.openproduct.client.domain.OpenProductToegestaneStatus
import nl.nlportal.openproduct.service.OpenProductService
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.util.UUID

/**
 * Binding data from Open Product: the subject's products for dynamic portal pages, one owned product
 * with its product type's content element as template for pages requested with an id.
 *
 * Product owners (`eigenaren`) are never returned.
 */
class OpenProductPortalPageBindingResolver(
    private val openProductService: OpenProductService,
) : PortalPageBindingResolver {
    override val type = "openproduct"

    override val fixedParams = setOf(PRODUCT_TYPE_CODE, PRODUCT_TYPE_CODES, CONTENT_LABEL)

    override fun validate(params: PortalPageParams): List<String> =
        buildList {
            checkInt(params, PAGE, "$PAGE must be >= 0") { it >= 0 }
            checkInt(params, SIZE, "$SIZE must be between 1 and $MAX_SIZE") { it in 1..MAX_SIZE }
            params.string(FILTER_STATUS)?.let { if (status(it) == null) add("$FILTER_STATUS '$it' is not a known status") }
        }

    override suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
    ): PortalPageBindingData {
        val authentication =
            context.authentication as? CommonGroundAuthentication
                ?: throw IllegalStateException("Open Product pages require a CommonGroundAuthentication")

        val result =
            openProductService.getProducten(
                authentication = authentication,
                pageNumber = (params.int(PAGE) ?: DEFAULT_PAGE) + 1,
                pageSize = params.int(SIZE) ?: DEFAULT_SIZE,
                status = params.string(FILTER_STATUS)?.let { status(it) },
                productTypeCode = params.string(PRODUCT_TYPE_CODE),
                productTypeCodes = params.list(PRODUCT_TYPE_CODES).ifEmpty { null },
            )

        val data = Mapper.get().valueToTree<ObjectNode>(result)
        data.get("results")?.forEach { withoutOwners(it) }
        return PortalPageBindingData(data = data)
    }

    override suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
        id: String,
    ): PortalPageBindingData? {
        val authentication = context.authentication as? CommonGroundAuthentication ?: return null
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return null
        val product = openProductService.findOwnedProduct(authentication, uuid) ?: return null

        val code = product.producttype.code
        if (params.string(PRODUCT_TYPE_CODE)?.let { it != code } == true) return null
        if (params.list(PRODUCT_TYPE_CODES).let { it.isNotEmpty() && code !in it }) return null

        val template =
            params.string(CONTENT_LABEL)?.let {
                openProductService.getProductTypeTemplate(product.producttype.uuid, context.locale.language, it)
            }
        return PortalPageBindingData(data = withoutOwners(Mapper.get().valueToTree(product)), template = template)
    }

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

    private fun status(value: String): OpenProductToegestaneStatus? = OpenProductToegestaneStatus.entries.firstOrNull { it.status.equals(value, ignoreCase = true) }

    private fun withoutOwners(product: JsonNode): JsonNode {
        (product as? ObjectNode)?.remove(OWNERS)
        return product
    }

    companion object {
        const val PRODUCT_TYPE_CODE = "product-type-code"
        const val PRODUCT_TYPE_CODES = "product-type-codes"
        const val FILTER_STATUS = "filter.status"
        const val PAGE = "page"
        const val SIZE = "size"
        const val CONTENT_LABEL = "content-label"
        private const val OWNERS = "eigenaren"
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SIZE = 10
        private const val MAX_SIZE = 100
    }
}