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
package nl.nlportal.core.frontend.page

import net.thisptr.jackson.jq.v2.core.Environment
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder
import net.thisptr.jackson.jq.v2.core.JsonQuery
import net.thisptr.jackson.jq.v2.core.version.Versions
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.JsonNodeFactory

/**
 * Runs the jq `projection` of page bindings on resolver data.
 * Expressions are compiled on every call. The only class that uses jackson-jq.
 */
class PageProjection {
    private val environment: Environment<JsonNode> =
        EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build()

    /**
     * @throws Exception when [expression] doesn't compile
     */
    fun compile(expression: String) {
        query(expression)
    }

    /**
     * Applies [expression] to [data]: one output is returned as is, no output becomes `null`,
     * several outputs become an array.
     *
     * @throws Exception when [expression] doesn't compile or fails on [data]
     */
    fun apply(
        expression: String,
        data: JsonNode,
    ): JsonNode {
        val outputs = query(expression).apply(data)
        return when (outputs.size) {
            0 -> JsonNodeFactory.instance.nullNode()
            1 -> outputs.single()
            else -> JsonNodeFactory.instance.arrayNode().addAll(outputs)
        }
    }

    private fun query(expression: String): JsonQuery<JsonNode> = environment.compile(expression)
}