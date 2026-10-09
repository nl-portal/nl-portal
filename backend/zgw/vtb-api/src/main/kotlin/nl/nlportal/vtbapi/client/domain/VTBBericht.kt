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
import java.time.LocalDateTime

class VTBBericht(
    val onderwerp: String,
    val berichtTekst: String,
    val publicatiedatum: LocalDateTime? = null,
    val referentie: String? = null,
    val ontvanger: String,
    val geopendOp: LocalDateTime? = null,
    val berichtType: String,
    val isGerelateerdAan: List<IsGerelateerdAan>? = emptyList(),
    val einddatumHandelingsTermijn: LocalDateTime? = null,
    val bijlagen: List<Bijlage> = emptyList(),
)

data class IsGerelateerdAan(
    val urn: String,
)

enum class VTBBerichtFilters(
    @JsonValue val value: String,
) : VTBFilters {
    PAGE("page"),
    PAGE_SIZE("page_size"),
    GEOPENDOP_NULL("geopendOp__isnull "),
    ONTVANGER("ontvanger"),
    PUBLICATIEDATUM_GTE("publicatiedatum__gte"),
    ;

    override fun toString() = this.value
}