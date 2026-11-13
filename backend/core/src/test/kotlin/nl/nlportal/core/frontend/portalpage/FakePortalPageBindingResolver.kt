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
package nl.nlportal.core.frontend.portalpage

import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingData
import nl.nlportal.core.frontend.portalpage.binding.PortalPageRequestContext
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.binding.PortalPageParams
import nl.nlportal.core.util.Mapper
import tools.jackson.databind.JsonNode

/**
 * Test resolver: list data echoes the params it received, by-id data comes from [objects].
 */
class FakePortalPageBindingResolver(
    override val type: String = "fake",
    override val fixedParams: Set<String> = setOf("source"),
    private val objects: Map<String, PortalPageBindingData> = emptyMap(),
) : PortalPageBindingResolver {
    val received = mutableListOf<PortalPageParams>()
    val contexts = mutableListOf<PortalPageRequestContext>()
    var failure: Exception? = null

    override fun validate(params: PortalPageParams): List<String> =
        buildList {
            params.int("page")?.let { if (it < 0) add("page must be >= 0") }
        }

    override suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
    ): PortalPageBindingData {
        received += params
        contexts += context
        failure?.let { throw it }
        val data =
            Mapper.get().createObjectNode().apply {
                put("count", 23)
                put("page", params.string("page"))
                put("status", params.string("filter.status"))
                put("secret", "not projected")
            }
        return PortalPageBindingData(data = data)
    }

    override suspend fun resolve(
        context: PortalPageRequestContext,
        params: PortalPageParams,
        id: String,
    ): PortalPageBindingData? {
        contexts += context
        failure?.let { throw it }
        return objects[id]
    }

    companion object {
        fun json(value: String): JsonNode = Mapper.get().readTree(value)
    }
}