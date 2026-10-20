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
package nl.nlportal.case.autoconfigure

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.case.autodeployment.CaseDefinitionApplicationReadyEventListener
import nl.nlportal.case.repository.CaseDefinitionRepository
import nl.nlportal.case.repository.CaseRepository
import nl.nlportal.case.service.CaseDefinitionService
import nl.nlportal.case.service.CaseService
import nl.nlportal.data.liquibase.LiquibaseMasterChangeLogLocation
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@AutoConfiguration
@EnableJpaRepositories(basePackages = ["nl.nlportal.case.repository"])
@EntityScan("nl.nlportal.case.domain")
class CaseAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(CaseDefinitionService::class)
    fun caseDefinitionService(
        caseDefinitionRepository: CaseDefinitionRepository,
        resourceLoader: ResourceLoader,
    ): CaseDefinitionService = CaseDefinitionService(caseDefinitionRepository, resourceLoader)

    @Bean
    @ConditionalOnMissingBean(CaseService::class)
    fun caseService(
        caseRepository: CaseRepository,
        caseDefinitionService: CaseDefinitionService,
    ): CaseService = CaseService(caseRepository, caseDefinitionService)

    @Bean
    @ConditionalOnMissingBean(CaseDefinitionApplicationReadyEventListener::class)
    fun caseDefinitionApplicationReadyEventListener(
        caseDefinitionService: CaseDefinitionService,
    ): CaseDefinitionApplicationReadyEventListener = CaseDefinitionApplicationReadyEventListener(caseDefinitionService)

    @Bean
    fun caseLiquibaseConfig(): LiquibaseMasterChangeLogLocation = LiquibaseMasterChangeLogLocation("config/liquibase/case-master.xml")

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}