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
package nl.nlportal.core.frontend.menu

import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendMenuEntryProperties

/**
 * A menu entry `route.type` served by a feature. A feature registers its bean only while it's enabled;
 * entries whose type has no bean are left out of the menu.
 */
interface FrontendMenuRouteType {
    val type: String

    /** Problems of [entry] for this type, e.g. a page that doesn't exist. Checked at startup, after a refresh and on every menu request. */
    fun problems(entry: FrontendMenuEntryProperties): List<String> = emptyList()
}