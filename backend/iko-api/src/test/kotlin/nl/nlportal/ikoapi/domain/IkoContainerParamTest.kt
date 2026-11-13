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

import nl.nlportal.core.util.Mapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class IkoContainerParamTest {
    @Test
    fun `writes a paged pageable with sort as an array of orders`() {
        val param =
            IkoContainerParam(
                containerId = "zaken",
                pageable = PageRequest.of(2, 10, Sort.by(Sort.Order.desc("identificatie"), Sort.Order.asc("startdatum"))),
                filters = mapOf("zaaktype" to "x"),
            )

        assertThat(json(param)).isEqualTo(
            """{"containerId":"zaken","pageable":{"pageNumber":2,"pageSize":10,"sort":[""" +
                """{"property":"identificatie","direction":"DESC"},{"property":"startdatum","direction":"ASC"}]},""" +
                """"filters":{"zaaktype":"x"}}""",
        )
    }

    @Test
    fun `writes non-default ignoreCase and nullHandling`() {
        val order =
            Sort.Order
                .asc("naam")
                .ignoreCase()
                .nullsLast()

        assertThat(json(IkoContainerParam("zaken", PageRequest.of(0, 10, Sort.by(order)))))
            .isEqualTo(
                """{"containerId":"zaken","pageable":{"pageNumber":0,"pageSize":10,"sort":[""" +
                    """{"property":"naam","direction":"ASC","ignoreCase":true,"nullHandling":"NULLS_LAST"}]}}""",
            )
    }

    @Test
    fun `omits an unsorted sort`() {
        assertThat(json(IkoContainerParam("zaken", PageRequest.of(0, 10))))
            .isEqualTo("""{"containerId":"zaken","pageable":{"pageNumber":0,"pageSize":10}}""")
    }

    @Test
    fun `omits an unpaged pageable and empty filters`() {
        assertThat(json(IkoContainerParam("zaken", Pageable.unpaged(Sort.by("identificatie")))))
            .isEqualTo("""{"containerId":"zaken"}""")
    }

    private fun json(param: IkoContainerParam) = Mapper.get().writeValueAsString(param.toPayload())
}