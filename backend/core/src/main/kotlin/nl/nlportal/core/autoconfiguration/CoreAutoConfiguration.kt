/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.autoconfiguration

import nl.nlportal.core.frontend.configuration.FrontendBindFailures
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.frontend.configuration.PortalPagesConfigurationProperties
import nl.nlportal.core.frontend.menu.FrontendMenuAudienceResolver
import nl.nlportal.core.frontend.menu.FrontendMenuConditionalChecker
import nl.nlportal.core.frontend.menu.FrontendMenuRouteType
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.menu.service.FrontendMenuConditionalCheckers
import nl.nlportal.core.frontend.menu.web.rest.FrontendMenuResource
import nl.nlportal.core.frontend.portalpage.PortalPageConfigValidator
import nl.nlportal.core.frontend.portalpage.PortalPageMenuRouteType
import nl.nlportal.core.frontend.portalpage.PortalPageProjection
import nl.nlportal.core.frontend.portalpage.PortalPageService
import nl.nlportal.core.frontend.portalpage.PortalPagesStartupValidation
import nl.nlportal.core.frontend.portalpage.binding.PortalPageBindingResolver
import nl.nlportal.core.frontend.portalpage.template.FilePortalPageTemplateSource
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateLoader
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateSource
import nl.nlportal.core.frontend.portalpage.web.rest.PortalPageResource
import nl.nlportal.core.frontend.service.FrontendConfigurationService
import nl.nlportal.core.frontend.web.rest.FrontendConfigurationResource
import nl.nlportal.core.frontend.web.rest.PingResource
import nl.nlportal.core.util.Mapper
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration
@EnableConfigurationProperties(
    FrontendModuleConfigurationProperties::class,
    FrontendConfigurationProperties::class,
    PortalPagesConfigurationProperties::class,
)
class CoreAutoConfiguration {
    @Bean("objectMapper")
    @ConditionalOnMissingBean(name = ["jsonMapper"])
    fun objectMapper(): JsonMapper = Mapper.get()

    @Bean
    @ConditionalOnMissingBean(FrontendConfigurationService::class)
    fun frontendConfigurationService(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
    ) = FrontendConfigurationService(
        frontendConfigurationProperties = frontendConfigurationProperties,
        frontendModuleConfigurationProperties = frontendModuleConfigurationProperties,
    )

    @Bean
    @ConditionalOnMissingBean(FrontendConfigurationResource::class)
    fun frontendConfigurationResource(
        frontendConfigurationService: FrontendConfigurationService,
    ) = FrontendConfigurationResource(frontendConfigurationService)

    @Bean
    @ConditionalOnMissingBean(FrontendBindFailures::class)
    fun frontendBindFailures() = FrontendBindFailures()

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal.frontend.toggles", name = ["menu-entries-enabled"], havingValue = "true")
    @ConditionalOnMissingBean(FrontendMenuConditionalCheckers::class)
    fun frontendMenuConditionalCheckers(
        checkers: ObjectProvider<FrontendMenuConditionalChecker>,
    ) = FrontendMenuConditionalCheckers(checkers.orderedStream().toList())

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal.frontend.toggles", name = ["menu-entries-enabled"], havingValue = "true")
    @ConditionalOnMissingBean(FrontendMenuService::class)
    fun frontendMenuService(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        frontendMenuAudienceResolver: FrontendMenuAudienceResolver?,
        frontendMenuConditionalCheckers: FrontendMenuConditionalCheckers,
        frontendMenuRouteTypes: ObjectProvider<FrontendMenuRouteType>,
        frontendBindFailures: FrontendBindFailures,
    ) = FrontendMenuService(
        properties = frontendConfigurationProperties,
        audienceResolver = frontendMenuAudienceResolver,
        conditionalCheckers = frontendMenuConditionalCheckers,
        routeTypes = frontendMenuRouteTypes.orderedStream().toList(),
        bindFailures = frontendBindFailures,
    )

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal.frontend.toggles", name = ["menu-entries-enabled"], havingValue = "true")
    @ConditionalOnMissingBean(FrontendMenuResource::class)
    fun frontendMenuResource(
        frontendMenuService: FrontendMenuService,
    ) = FrontendMenuResource(frontendMenuService)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageProjection::class)
    fun portalPageProjection() = PortalPageProjection()

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(FilePortalPageTemplateSource::class)
    fun filePortalPageTemplateSource(portalPagesConfigurationProperties: PortalPagesConfigurationProperties) = FilePortalPageTemplateSource(portalPagesConfigurationProperties.properties.templateLocations)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageTemplateLoader::class)
    fun portalPageTemplateLoader(portalPageTemplateSources: ObjectProvider<PortalPageTemplateSource>) = PortalPageTemplateLoader(portalPageTemplateSources.orderedStream().toList())

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageConfigValidator::class)
    fun portalPageConfigValidator(
        portalPageBindingResolvers: ObjectProvider<PortalPageBindingResolver>,
        portalPageProjection: PortalPageProjection,
        portalPageTemplateLoader: PortalPageTemplateLoader,
        frontendBindFailures: FrontendBindFailures,
    ) = PortalPageConfigValidator(portalPageBindingResolvers.orderedStream().toList(), portalPageProjection, portalPageTemplateLoader.sources, frontendBindFailures)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageMenuRouteType::class)
    fun portalPageMenuRouteType(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        portalPageConfigValidator: PortalPageConfigValidator,
    ) = PortalPageMenuRouteType(frontendConfigurationProperties, portalPageConfigValidator)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageService::class)
    fun portalPageService(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        frontendMenuService: FrontendMenuService,
        portalPageBindingResolvers: ObjectProvider<PortalPageBindingResolver>,
        portalPageProjection: PortalPageProjection,
        portalPageConfigValidator: PortalPageConfigValidator,
        portalPageTemplateLoader: PortalPageTemplateLoader,
    ) = PortalPageService(
        properties = frontendConfigurationProperties,
        menuService = frontendMenuService,
        resolvers = portalPageBindingResolvers.orderedStream().toList(),
        projection = portalPageProjection,
        validator = portalPageConfigValidator,
        templateLoader = portalPageTemplateLoader,
    )

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPageResource::class)
    fun portalPageResource(portalPageService: PortalPageService) = PortalPageResource(portalPageService)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal", name = [PORTAL_PAGES_ENABLED, MENU_ENTRIES_ENABLED], havingValue = "true")
    @ConditionalOnMissingBean(PortalPagesStartupValidation::class)
    fun portalPagesStartupValidation(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        portalPageConfigValidator: PortalPageConfigValidator,
    ) = PortalPagesStartupValidation(frontendConfigurationProperties, portalPageConfigValidator)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal.support", name = ["ping-endpoint.enabled"], havingValue = "true")
    @ConditionalOnMissingBean(PingResource::class)
    fun pingResource(): PingResource = PingResource()

    companion object {
        const val PORTAL_PAGES_ENABLED = "config.portal-pages.enabled"
        const val MENU_ENTRIES_ENABLED = "frontend.toggles.menu-entries-enabled"
    }
}