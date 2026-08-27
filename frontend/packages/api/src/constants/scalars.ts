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
export const scalarConfig = {
  strictScalars: true,
  scalars: {
    ID: {
      input: "string",
      output: "string",
    },
    UUID: {
      input: "string",
      output: "string",
    },
    PositiveFloat: {
      input: "number",
      output: "number",
    },
    Date: {
      input: "string",
      output: "string",
    },
    DateTime: {
      input: "string",
      output: "string",
    },
    LocalDateTime: {
      input: "string",
      output: "string",
    },
    LocalTime: {
      input: "string",
      output: "string",
    },
    ZonedDateTime: {
      input: "string",
      output: "string",
    },
    Locale: {
      input: "string",
      output: "string",
    },
    JSON: {
      input: "any",
      output: "any",
    },
    BigDecimal: {
      input: "number",
      output: "number",
    },
    BigInteger: {
      input: "number",
      output: "number",
    },
    Long: {
      input: "number",
      output: "number",
    },
  },
};
