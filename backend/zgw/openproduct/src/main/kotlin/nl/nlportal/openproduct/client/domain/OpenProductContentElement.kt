package nl.nlportal.openproduct.client.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class OpenProductContentElement(
    val uuid: UUID,
    val labels: List<String>? = emptyList(),
    @JsonProperty("aanvullende_informatie")
    val aanvullendeInformatie: String? = null,
    val content: String,
    val taal: String,
)

enum class OpenProductContentElementsFilters(
    @JsonValue val value: String,
) : OpenProductFilters {
    PAGE("page"),
    PAGE_SIZE("page_size"),
    PRODUCTTYPE_UUID("producttype__uuid"),
    ;

    override fun toString() = this.value
}
