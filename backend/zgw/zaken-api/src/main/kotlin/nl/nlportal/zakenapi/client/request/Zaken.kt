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
package nl.nlportal.zakenapi.client.request

import nl.nlportal.zakenapi.domain.Zaak
import java.util.UUID

interface Zaken {
    fun search(): SearchZaken

    fun get(id: UUID): GetZaak
}

interface SearchZaken :
    PagedRetrieve<SearchZaken, Zaak>,
    AuthenticationFilter<SearchZaken> {
    fun ofZaakType(zaakType: String): SearchZaken

    fun ofZaakTypes(zaakTypeIds: List<UUID>): SearchZaken

    fun notInZaakTypes(zaakTypeIds: List<UUID>): SearchZaken

    fun isOpen(open: Boolean): SearchZaken

    fun ofIdentificatie(identificatie: String): SearchZaken

    fun withRolOmschrijvingGeneriek(): SearchZaken

    fun ofOmschrijving(omschrijving: String): SearchZaken

    fun ofIdenfitifactieContains(identificatie: String): SearchZaken
}

interface GetZaak : Retrieve<Zaak>