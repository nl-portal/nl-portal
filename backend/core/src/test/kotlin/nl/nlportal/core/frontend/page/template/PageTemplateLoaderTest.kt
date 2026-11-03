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
package nl.nlportal.core.frontend.page.template

import kotlinx.coroutines.test.runTest
import nl.nlportal.core.frontend.page.FakePageTemplateSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PageTemplateLoaderTest {
    private val source = FakePageTemplateSource(documents = mapOf("a" to PageTemplateDocument("a")))
    private val other = FakePageTemplateSource(type = "other", documents = mapOf("a" to PageTemplateDocument("other a")))
    private val loader = PageTemplateLoader(listOf(source, other))

    @Test
    fun `the document comes from the source of the type`() =
        runTest {
            assertEquals("a", loader.load("fake", mapOf("name" to "a"))?.template)
            assertEquals("other a", loader.load("other", mapOf("name" to "a"))?.template)
        }

    @Test
    fun `a changed document shows at the next load`() =
        runTest {
            loader.load("fake", mapOf("name" to "a"))
            source.documents = mapOf("a" to PageTemplateDocument("changed"))

            assertEquals("changed", loader.load("fake", mapOf("name" to "a"))?.template)
            assertEquals(2, source.loads)
        }

    @Test
    fun `missing documents and failures are passed through`() =
        runTest {
            assertNull(loader.load("fake", mapOf("name" to "b")))
            source.failure = IllegalStateException("down")
            assertThrows<IllegalStateException> { loader.load("fake", mapOf("name" to "a")) }
        }

    @Test
    fun `an unknown source throws`() =
        runTest {
            assertThrows<NoSuchElementException> { loader.load("nope", emptyMap()) }
        }
}