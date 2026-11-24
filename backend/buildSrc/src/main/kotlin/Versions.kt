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
object Versions {
    const val everitJsonSchema = "1.14.6"
    const val jsonPath = "3.0.0"
    const val kotlinCoroutines = "1.11.0"
    const val apacheTikaVersion = "4.2.0"
    const val clamAvVersion = "2.1.2"
    const val apacheCommons = "3.22.0"
    const val commonsIo = "2.22.0"
    const val commonsCodec = "1.23.0"
    const val graphqlJavaScalars = "24.0"
    const val kotlinLogging = "8.0.7"
    const val springCloud = "5.1.0"
    const val netty = "4.2.21.Final" // override Boot 4.1.0 BOM (4.2.15.Final), CVE-2026-44891/-55831/-55833 fix

    // Test versions
    const val hamcrest = "3.0"
    const val mockitoKotlin = "6.4.0"
    const val okHttp3 = "5.5.1"
    const val postgresql = "42.7.14"
    const val h2 = "2.5.252"
}
