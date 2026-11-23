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
package nl.nlportal.core.frontend.portalpage

import nl.nlportal.core.autoconfiguration.CoreAutoConfiguration
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.menu.FrontendMenuService
import nl.nlportal.core.frontend.portalpage.template.PortalPageTemplateLoader
import nl.nlportal.core.frontend.portalpage.web.rest.PortalPageResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * The portal pages module runs only with both `nl-portal.config.portal-pages.enabled` and menu entries enabled.
 */
internal class PortalPagesAutoConfigurationTest {
    private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CoreAutoConfiguration::class.java))

    @Test
    fun `portal pages run with the module and menu entries enabled`() {
        runner
            .withPropertyValues("nl-portal.config.portal-pages.enabled=true", "nl-portal.frontend.toggles.menu-entries-enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(PortalPageService::class.java)
                assertThat(context).hasSingleBean(PortalPageResource::class.java)
                assertThat(context).hasSingleBean(PortalPageTemplateLoader::class.java)
            }
    }

    @Test
    fun `portal pages are off without the module, and their menu entries are left out`() {
        runner
            .withPropertyValues("nl-portal.frontend.toggles.menu-entries-enabled=true", *PORTAL_PAGE_ENTRY)
            .run { context ->
                assertThat(context).doesNotHaveBean(PortalPageService::class.java)
                assertThat(context).doesNotHaveBean(PortalPageResource::class.java)
                assertThat(context).doesNotHaveBean(PortalPageMenuRouteType::class.java)
                assertThat(context.getBean(FrontendMenuService::class.java).problems().keys).containsExactly(0)
            }
    }

    @Test
    fun `the context starts with broken menu entries and pages for every flag combination`() {
        listOf(true, false).forEach { menuEntries ->
            listOf(true, false).forEach { portalPages ->
                runner
                    .withPropertyValues(
                        "nl-portal.frontend.toggles.menu-entries-enabled=$menuEntries",
                        "nl-portal.config.portal-pages.enabled=$portalPages",
                        *BROKEN,
                    ).run { context -> assertThat(context).hasNotFailed() }
            }
        }
    }

    @Test
    fun `values Spring can't read leave the entry or page out instead of failing the context`() {
        runner
            .withPropertyValues("nl-portal.config.portal-pages.enabled=true", "nl-portal.frontend.toggles.menu-entries-enabled=true", *BROKEN)
            .run { context ->
                assertThat(context).hasNotFailed()
                val problems = context.getBean(FrontendMenuService::class.java).problems()
                assertThat(problems[0]).anyMatch { it.startsWith("could not read menu-entries[0].ordering") }
                assertThat(problems[1]).anyMatch { it.startsWith("could not read menu-entries[1].hidden") }
                assertThat(problems[2]).anyMatch { it.startsWith("could not read menu-entries[2].conditional") }
                assertThat(problems[3]).containsExactly("page 'over-ons' is invalid, see pages.over-ons")
                val validator = context.getBean(PortalPageConfigValidator::class.java)
                val properties = context.getBean(FrontendConfigurationProperties::class.java)
                assertThat(validator.allProblems(properties.pages)).anyMatch { it.startsWith("pages.over-ons: could not read pages.over-ons.template") }
            }
    }

    @Test
    fun `values outside menu entries and pages still fail the context`() {
        runner
            .withPropertyValues("nl-portal.frontend.properties.message-count-polling-interval=abc")
            .run { context -> assertThat(context).hasFailed() }
    }

    @Test
    fun `portal pages are off without menu entries`() {
        runner
            .withPropertyValues("nl-portal.config.portal-pages.enabled=true")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean(PortalPageService::class.java)
            }
    }

    companion object {
        private val PORTAL_PAGE_ENTRY =
            arrayOf(
                "nl-portal.frontend.menu-entries[0].name=over-ons",
                "nl-portal.frontend.menu-entries[0].route.type=portal-page",
                "nl-portal.frontend.menu-entries[0].route.page=over-ons",
                "nl-portal.frontend.menu-entries[0].route.path=/over-ons",
            )
        private val BROKEN =
            arrayOf(
                "nl-portal.frontend.menu-entries[0].name=a",
                "nl-portal.frontend.menu-entries[0].ordering=first",
                "nl-portal.frontend.menu-entries[0].route.type=portal-page",
                "nl-portal.frontend.menu-entries[0].route.page=over-ons",
                "nl-portal.frontend.menu-entries[0].route.path=/a",
                "nl-portal.frontend.menu-entries[1].name=b",
                "nl-portal.frontend.menu-entries[1].hidden=maybe",
                "nl-portal.frontend.menu-entries[1].route.type=portal-page",
                "nl-portal.frontend.menu-entries[1].route.page=over-ons",
                "nl-portal.frontend.menu-entries[1].route.path=/b",
                "nl-portal.frontend.menu-entries[2].name=c",
                "nl-portal.frontend.menu-entries[2].conditional=thema",
                "nl-portal.frontend.menu-entries[2].route.type=portal-page",
                "nl-portal.frontend.menu-entries[2].route.page=over-ons",
                "nl-portal.frontend.menu-entries[2].route.path=/c",
                "nl-portal.frontend.menu-entries[3].name=d",
                "nl-portal.frontend.menu-entries[3].route.type=portal-page",
                "nl-portal.frontend.menu-entries[3].route.page=over-ons",
                "nl-portal.frontend.menu-entries[3].route.path=/d",
                "nl-portal.frontend.pages.over-ons.type=static",
                "nl-portal.frontend.pages.over-ons.template=inline",
            )
    }
}