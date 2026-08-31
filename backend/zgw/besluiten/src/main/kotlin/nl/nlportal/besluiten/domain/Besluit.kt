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

import java.time.LocalDate

data class Besluit(
    val url: String,
    val identificatie: String,
    val verantwoordelijkeOrganisatie: String,
    val besluittype: String,
    val zaak: String? = null,
    val datum: LocalDate,
    val toelichting: String?,
    val bestuursorgaan: String?,
    val ingangsdatum: LocalDate,
    val vervaldatum: LocalDate?,
    val vervalreden: String,
    val vervalredenWeergave: String,
    val publicatiedatum: LocalDate?,
    val verzenddatum: LocalDate?,
    val uiterlijkeReactiedatum: LocalDate?,
)