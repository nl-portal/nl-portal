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

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import nl.nlportal.case.domain.Case
import nl.nlportal.case.domain.CaseDefinition
import nl.nlportal.case.domain.CaseDefinitionId
import nl.nlportal.case.domain.CaseId
import nl.nlportal.case.domain.Status
import nl.nlportal.case.domain.Submission
import nl.nlportal.case.repository.CaseRepository
import org.json.JSONObject
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

@Transactional
class CaseService(
    private val caseRepository: CaseRepository,
    private val caseDefinitionService: CaseDefinitionService,
) {
    fun create(
        caseDefinitionId: String,
        submission: ObjectNode,
        authentication: Authentication,
        initialStatus: String? = null,
    ): Case {
        val caseDefinition = caseDefinitionService.findById(CaseDefinitionId.existingId(caseDefinitionId))!!
        val messageData = submission.deepCopy()
        val caseData = validateSubmissionAgainstSchema(submission, caseDefinition)
        val status: Status =
            if (initialStatus == null) {
                Status(caseDefinition.statusDefinition.statuses.first())
            } else {
                caseDefinition.statusDefinition.validateStatus(initialStatus)
                Status(initialStatus)
            }
        val case =
            Case(
                caseId = CaseId.newId(UUID.randomUUID()),
                userId = authentication.name,
                status = status,
                submission = Submission(caseData),
                caseDefinitionId = caseDefinition.caseDefinitionId,
            )
        caseRepository.save(case)
        return case
    }

    fun getAllCases(userId: String): List<Case> = caseRepository.findAllByUserId(userId)

    fun getCase(
        id: CaseId,
        userId: String,
    ): Case? = caseRepository.findCaseByCaseIdAndUserId(id, userId)

    fun getCase(externalId: String): Case? = caseRepository.findCaseByExternalId(externalId)

    private fun validateSubmissionAgainstSchema(
        submission: ObjectNode,
        caseDefinition: CaseDefinition,
    ): ObjectNode {
        logger.debug { "Validating submission against json schema: ${caseDefinition.caseDefinitionId.value}" }
        val keys = getKeys(caseDefinition.schema.value, mutableListOf())

        submission.retain(keys)
        caseDefinition.schema.validateCase(JSONObject(submission.toString()))

        return submission
    }

    private fun getKeys(
        json: JsonNode,
        keys: MutableList<String>,
    ): List<String> {
        if (json.isObject) {
            json.properties().forEach { field ->
                keys.add(field.key)
                getKeys(field.value, keys)
            }
        }
        if (json.isArray) {
            json.forEach { node -> getKeys(node, keys) }
        }
        if (json.isValueNode) {
            keys.add(json.asString())
        }
        return keys
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}