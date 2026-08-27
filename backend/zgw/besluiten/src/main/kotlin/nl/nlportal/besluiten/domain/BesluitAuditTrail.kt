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
package nl.nlportal.besluiten.domain

import tools.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID

data class BesluitAuditTrail(
    val uuid: UUID,
    val bron: String,
    val applicatieId: String?,
    val applicatieWeergave: String?,
    val gebruikersId: String?,
    val gebruikersWeergave: String?,
    val actie: String,
    val actieWeergave: String?,
    val resultaat: Int,
    val hoofdObject: String,
    val resource: String,
    val resourceUrl: String,
    val toelichting: String?,
    val resourceWeergave: String,
    val aanmaakdatum: LocalDateTime?,
    val wijzigingen: BesluitAuditTrailWijzigingen,
)

data class BesluitAuditTrailWijzigingen(
    val oud: ObjectNode?,
    val nieuw: ObjectNode?,
)