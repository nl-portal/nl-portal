/*
 * Copyright 2026 Ritense BV, the Netherlands.
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

import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import nl.nlportal.core.util.Mapper
import nl.nlportal.core.util.Mapper.DATE_TIME_FORMAT
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer

@JacksonComponent
class CustomLocalDateTime {
    // Custom Serializer
    internal class LocalDateTimeSerializer : ValueSerializer<LocalDateTime?>() {
        @Throws(JacksonException::class)
        override fun serialize(
            value: LocalDateTime?,
            gen: JsonGenerator,
            ctxt: SerializationContext?,
        ) {
            gen.writeString(value?.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        }
    }

    // Custom Deserializer
    internal class LocalDateDeserializer : ValueDeserializer<LocalDateTime?>() {
        @Throws(IOException::class)
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext?,
        ): LocalDateTime = LocalDateTime.parse(p.valueAsString, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
    }
}