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

import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PageProjectionTest {
    private val projection = PageProjection()
    private val data = json("""{"count":2,"results":[{"uuid":"a","naam":"x","eigenaren":["1"]},{"uuid":"b","naam":"y"}]}""")

    @Test
    fun `one output is returned as is`() {
        assertEquals(json("""[{"id":"a","naam":"x"},{"id":"b","naam":"y"}]"""), projection.apply("[.results[] | {id: .uuid, naam}]", data))
    }

    @Test
    fun `no output becomes null`() {
        assertTrue(projection.apply("empty", data).isNull)
    }

    @Test
    fun `several outputs become an array`() {
        assertEquals(json("""["a","b"]"""), projection.apply(".results[].uuid", data))
    }

    @Test
    fun `the default projection exposes nothing`() {
        assertEquals(json("{}"), projection.apply("{}", data))
    }

    @Test
    fun `a compile error throws`() {
        assertThrows<Exception> { projection.compile("{ naam") }
        assertThrows<Exception> { projection.apply("{ naam", data) }
    }

    private fun json(value: String) = Mapper.get().readTree(value)
}