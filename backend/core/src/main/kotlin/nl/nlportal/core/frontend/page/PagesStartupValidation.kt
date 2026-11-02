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
package nl.nlportal.core.frontend.page

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

/**
 * Logs every problem in `nl-portal.frontend.pages` once at startup. Problems never stop startup: at request time
 * an invalid page answers 404 and an invalid binding answers `error: true`.
 */
class PagesStartupValidation(
    private val properties: FrontendConfigurationProperties,
    private val validator: PageConfigValidator,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun validate() {
        validator.allProblems(properties.pages).forEach { logger.warn { "Invalid nl-portal.frontend.$it" } }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}