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
export { default as LocaleContext } from "./contexts/LocaleContext";
export { default as MockWrapper } from "./mocks/MockWrapper";
export { default as LocalizationProvider } from "./providers/LocalizationProvider";
export { default as useDateFormatter } from "./hooks/useDateFormatter";
export { default as useActionLabels } from "./hooks/useActionLabels";
export * from "./interfaces/locales";
export * from "./interfaces/messages";
export * from "./i18n/default-locales";
export * from "./i18n/messages/messages";
export * from "./i18n/messages/nl-nl";
export * from "./i18n/messages/en-gb";
