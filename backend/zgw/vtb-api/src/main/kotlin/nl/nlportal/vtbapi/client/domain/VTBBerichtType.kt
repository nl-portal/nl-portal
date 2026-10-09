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
package nl.nlportal.vtbapi.client.domain

import com.fasterxml.jackson.annotation.JsonValue

data class VTBBerichtType(
    val bijlageType: List<Bijlage>,
    val handelingsPerspectief: HandelingsPerspectief,
    val mijnOverheidBerichtenbox: Boolean = false,
    val mijnOverheidBerichtenboxType: String? = null,
    val verantwoordelijkeOrganisatie: String,
)

enum class HandelingsPerspectief(
    @JsonValue val value: String,
) {
    BETALEN("betalen"),
    INCASSO("incasso"),
    INFORMATIE_GEVEN("informatie_geven"),
    INFORMATIE_KRIJGEN("informatie_krijgen"),
    REACTIE_ONTVANGEN("reactie_ontvangen"),
    VERNIEUWING_NODIG("vernieuwing_nodig"),
    UITNODIGING_VOOR_GESPREK("uitnodiging_voor_afspraak"),
    GEEN_HANDELINGS_PERSPECTIEF(""),
}