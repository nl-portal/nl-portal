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
package nl.nlportal.documentenapi.service

import nl.nlportal.documentenapi.domain.VirusScanResult
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux

interface VirusScanService {
    fun scan(content: ByteArray): VirusScanResult

    @Deprecated(
        message = "Flux is single-consumption — risks empty/partial scan when reused. Buffer content first and use scan(ByteArray).",
        replaceWith = ReplaceWith("scan(bufferedContent)"),
    )
    fun scan(content: Flux<DataBuffer>): VirusScanResult
}