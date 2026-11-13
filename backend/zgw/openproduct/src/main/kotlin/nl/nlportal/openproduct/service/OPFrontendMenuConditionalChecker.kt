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
package nl.nlportal.openproduct.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.frontend.menu.FrontendMenuConditionalChecker
import nl.nlportal.core.util.CoreUtils
import org.springframework.security.core.Authentication

/**
 * Menu condition `thema: <naam>`: holds when the user has at least one product in the published theme with that `naam`.
 * No theme, another kind of authentication or an Open Product error counts as `false`.
 */
class OPFrontendMenuConditionalChecker(
    private val openProductService: OpenProductService,
) : FrontendMenuConditionalChecker {
    override val key: String = "thema"

    override suspend fun check(
        authentication: Authentication,
        value: String,
    ): Boolean {
        val user = authentication as? CommonGroundAuthentication ?: return false
        return try {
            val thema = openProductService.findThemaByName(user, value) ?: return false
            openProductService.getThemaProducten(user, thema).isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn { "Menu condition thema '$value' failed: ${CoreUtils.logSummary(e)}" }
            logger.trace(e) { "Menu condition thema '$value' failed" }
            false
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}