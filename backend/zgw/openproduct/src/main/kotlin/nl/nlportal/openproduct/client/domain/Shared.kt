/*
 * Copyright 2024-2025 Ritense BV, the Netherlands.
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
package nl.nlportal.openproduct.client.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

interface OpenProductFilters

data class OpenProductUrl(
    val url: String? = null,
    val urn: String? = null,
)

data class OpenProductContentElement(
    val uuid: UUID,
    val labels: List<String>? = emptyList(),
    @JsonProperty("aanvullende_informatie")
    val aanvullendeInformatie: String? = null,
    val content: String,
    val taal: String,
)

enum class OpenProductToegestaneStatus(
    @JsonValue val status: String,
) {
    INITIEEL("initieel"),
    IN_AANVRAAG("in_aanvraag"),
    GEREED("gereed"),
    ACTIEF("actief"),
    INGETROKKEN("ingetrokken"),
    GEWEIGERD("geweigerd"),
    VERLOPEN("verlopen"),
}

enum class OpenProductDoelgroep(
    @JsonValue val status: String,
) {
    BURGERS("burgers"),
    INTERNE_ORGANISATIE("interne_organisatie"),
    SAMENWERKINGSPARTNERS("samenwerkingspartners"),
    BEDRIJVEN_EN_INSTELLINGEN("bedrijven_en_instellingen"),
}

enum class OpenProductFrequentie(
    @JsonValue val frequentie: String,
) {
    EENMALIG("eenmalig"),
    MAANDELIJKS("maandelijks"),
    JAARLIJKS("jaarlijks"),
    GEEN(""),
}

enum class SortList(
    @JsonValue val sort: String,
) {
    ASCENDING("ascending"),
    DESCENDING("descending"),
}