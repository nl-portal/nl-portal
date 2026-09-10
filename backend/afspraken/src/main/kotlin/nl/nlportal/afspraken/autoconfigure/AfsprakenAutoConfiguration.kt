package nl.nlportal.afspraken.autoconfigure

import nl.nlportal.afspraken.graphql.AfsprakenQuery
import nl.nlportal.afspraken.service.AfsprakenService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
@EnableConfigurationProperties(AfsprakenConfiguration::class)
@ConditionalOnProperty(prefix = "nl-portal.config.afspraken", name = ["enabled"], havingValue = "true")
class AfsprakenAutoConfiguration {
    @Bean
    fun afsprakenWebClient(afsprakenConfiguration: AfsprakenConfiguration): WebClient =
        WebClient.builder()
            .baseUrl(afsprakenConfiguration.apiUrl)
            .build()

    @Bean
    fun afsprakenService(afsprakenWebClient: WebClient): AfsprakenService =
        AfsprakenService(webClient = afsprakenWebClient)

    @Bean
    @ConditionalOnMissingBean(AfsprakenQuery::class)
    fun afsprakenQuery(afsprakenService: AfsprakenService): AfsprakenQuery =
        AfsprakenQuery(afsprakenService = afsprakenService)
}
