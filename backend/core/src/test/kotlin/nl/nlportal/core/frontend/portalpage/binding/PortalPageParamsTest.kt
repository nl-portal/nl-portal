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
package nl.nlportal.core.frontend.portalpage.binding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PortalPageParamsTest {
    @Test
    fun `keys are compared canonically`() {
        val params = PortalPageParams(mapOf("product-type-code" to "erfpacht", "filter.Status" to "open"))

        assertEquals("erfpacht", params.string("PRODUCTTYPECODE"))
        assertEquals("erfpacht", params.string("productTypeCode"))
        assertEquals("open", params.string("filter.status"))
    }

    @Test
    fun `blank values are absent`() {
        val params = PortalPageParams(mapOf("status" to "", "size" to " "))

        assertNull(params.string("status"))
        assertNull(params.int("size"))
    }

    @Test
    fun `int parses numbers and rejects anything else`() {
        val params = PortalPageParams(mapOf("page" to "2", "size" to "ten"))

        assertEquals(2, params.int("page"))
        assertNull(params.int("missing"))
        assertThrows<IllegalArgumentException> { params.int("size") }
    }

    @Test
    fun `list reads indexed keys in index order`() {
        val params = PortalPageParams(mapOf("codes.1" to "b", "codes.0" to " a ", "codes.10" to "k", "codes.2" to "c"))

        assertEquals(listOf("a", "b", "c", "k"), params.list("codes"))
    }

    @Test
    fun `list reads a comma-separated value`() {
        val params = PortalPageParams(mapOf("codes" to "a, b,,c "))

        assertEquals(listOf("a", "b", "c"), params.list("codes"))
    }

    @Test
    fun `list reads a single value and nothing`() {
        val params = PortalPageParams(mapOf("codes" to "a"))

        assertEquals(listOf("a"), params.list("codes"))
        assertEquals(emptyList<String>(), params.list("missing"))
    }

    @Test
    fun `withPrefix returns the keys under the prefix`() {
        val params = PortalPageParams(mapOf("filter.status" to "open", "filter.Product-Type" to "x", "filters" to "no", "page" to "0"))

        assertEquals(mapOf("status" to "open", "producttype" to "x"), params.withPrefix("filter"))
    }

    @Test
    fun `overrides replace values and indexed list items, blank keeps the default`() {
        val params = PortalPageParams(mapOf("page" to "0", "sort.0" to "a:asc", "sort.1" to "b:desc", "size" to "10"))

        val merged = params.withOverrides(mapOf("page" to "3", "sort" to "c:asc", "size" to "", "filter.status" to "open"))

        assertEquals(PortalPageParams(mapOf("page" to "3", "sort" to "c:asc", "size" to "10", "filter.status" to "open")), merged)
    }

    @Test
    fun `display joins lists`() {
        val params = PortalPageParams(mapOf("sort.0" to "a:asc", "sort.1" to "b:desc", "page" to "1"))

        assertEquals("a:asc, b:desc", params.display("sort"))
        assertEquals("1", params.display("page"))
        assertNull(params.display("missing"))
    }
}