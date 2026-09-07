package nl.nlportal.afspraken.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nl-portal.config.afspraken")
class AfsprakenConfiguration {
    var enabled: Boolean = false
    var apiUrl: String = ""
}
