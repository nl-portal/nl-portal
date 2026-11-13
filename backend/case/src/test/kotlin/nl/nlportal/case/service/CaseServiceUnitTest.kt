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
package nl.nlportal.case.service

import nl.nlportal.case.BaseTest
import nl.nlportal.core.util.Mapper
import org.assertj.core.api.Assertions.assertThat
import org.everit.json.schema.ValidationException
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import tools.jackson.databind.node.ObjectNode

class CaseServiceUnitTest : BaseTest() {
    lateinit var caseService: CaseService
    val initialStatus = "a"
    val user = "john@example.com"
    val caseDefinition = personCaseDefinition()
    val authentication = Mockito.mock(Authentication::class.java)

    @BeforeEach
    fun setup() {
        baseSetUp()
        `when`(caseDefinitionService.findById(caseDefinition.caseDefinitionId)).thenReturn(caseDefinition)
        caseService = CaseService(caseRepository, caseDefinitionService)
        `when`(authentication.name).thenReturn(user)
    }

    @Test
    fun `should not create case with empty submission`() {
        val submission = Mapper.get().readValue("{}", ObjectNode::class.java)
        val illegalStateException =
            assertThrows(IllegalStateException::class.java) {
                caseService.create(
                    caseDefinition.caseDefinitionId.value,
                    submission,
                    authentication,
                )
            }
        assertThat(illegalStateException).hasMessage("Empty case data")
    }

    @Test
    fun `should eliminate unknown properties from submission prior case def validation`() {
        val submission = Mapper.get().readValue("{\"unknownProperty\" : \"myName\"}", ObjectNode::class.java)
        val illegalStateException =
            assertThrows(IllegalStateException::class.java) {
                caseService.create(
                    caseDefinition.caseDefinitionId.value,
                    submission,
                    authentication,
                )
            }
        assertThat(illegalStateException).hasMessage("Empty case data")
    }

    @Test
    fun `should not create case due to size validation exception`() {
        val submission = Mapper.get().readValue("{\"firstName\" : \"moreThan15CharsTooLong\"}", ObjectNode::class.java)
        val validationException =
            assertThrows(ValidationException::class.java) {
                caseService.create(
                    caseDefinition.caseDefinitionId.value,
                    submission,
                    authentication,
                )
            }
        assertThat(validationException).hasMessage("#/firstName: expected maxLength: 15, actual: 22")
    }

    @Test
    fun `should not create case due to type validation exception`() {
        val submission = Mapper.get().readValue("{\"firstName\" : 1}", ObjectNode::class.java)
        val validationException =
            assertThrows(ValidationException::class.java) {
                caseService.create(
                    caseDefinition.caseDefinitionId.value,
                    submission,
                    authentication,
                )
            }
        assertThat(validationException).hasMessage("#/firstName: expected type: String, found: Integer")
    }

    @Test
    fun `should create case with valid submission`() {
        val submission = Mapper.get().readValue("{\"firstName\" : \"myName\"}", ObjectNode::class.java)
        val case =
            caseService.create(
                caseDefinition.caseDefinitionId.value,
                submission,
                authentication,
            )

        assertNotNull(case)
        assertThat(case.caseId).isNotNull
        assertThat(case.userId).isEqualTo(user)
        assertThat(case.status.name).isEqualTo(initialStatus)
        assertThat(
            case.submission.value
                .path("firstName")
                .stringValue(),
        ).isEqualTo("myName")
    }
}