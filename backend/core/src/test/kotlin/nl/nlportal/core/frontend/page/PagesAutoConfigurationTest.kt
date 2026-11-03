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
package nl.nlportal.core.frontend.page

import nl.nlportal.core.autoconfiguration.CoreAutoConfiguration
import nl.nlportal.core.frontend.page.template.PageTemplateLoader
import nl.nlportal.core.frontend.page.web.rest.FrontendPageResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * The pages module runs only with both `nl-portal.config.pages.enabled` and menu entries enabled.
 */
internal class PagesAutoConfigurationTest {
    private val runner = ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CoreAutoConfiguration::class.java))

    @Test
    fun `pages run with the module and menu entries enabled`() {
        runner
            .withPropertyValues("nl-portal.config.pages.enabled=true", "nl-portal.frontend.toggles.menu-entries-enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(PageService::class.java)
                assertThat(context).hasSingleBean(FrontendPageResource::class.java)
                assertThat(context).hasSingleBean(PageTemplateLoader::class.java)
            }
    }

    @Test
    fun `pages are off without the module`() {
        runner
            .withPropertyValues("nl-portal.frontend.toggles.menu-entries-enabled=true")
            .run { context ->
                assertThat(context).doesNotHaveBean(PageService::class.java)
                assertThat(context).doesNotHaveBean(FrontendPageResource::class.java)
            }
    }

    @Test
    fun `pages are off without menu entries`() {
        runner
            .withPropertyValues("nl-portal.config.pages.enabled=true")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean(PageService::class.java)
            }
    }
}