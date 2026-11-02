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
package nl.nlportal.core

import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder
import net.thisptr.jackson.jq.v2.core.version.Versions
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider
import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * jackson-jq 2.x is built against a newer Jackson 3 than the Spring Boot BOM pins.
 * Guards that it runs on the Jackson 3 version we actually ship, including the joni regex functions.
 */
class JacksonJqCompatibilityTest {
    private val environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build()

    @Test
    fun `projects Jackson 3 nodes`() {
        val input = Mapper.get().readTree("""{"count":2,"results":[{"uuid":"a","naam":"x","eigenaren":["1"]},{"uuid":"b","naam":"y"}]}""")

        val output = environment.compile("[.results[] | {id: .uuid, naam}]").apply(input)

        assertEquals(listOf(Mapper.get().readTree("""[{"id":"a","naam":"x"},{"id":"b","naam":"y"}]""")), output)
    }

    @Test
    fun `runs regex functions`() {
        val input = Mapper.get().readTree("""["erfpacht-1","parkeren"]""")

        val output = environment.compile("""map(select(test("^erf")))""").apply(input)

        assertEquals(listOf(Mapper.get().readTree("""["erfpacht-1"]""")), output)
    }
}