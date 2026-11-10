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
import { Paragraph } from "@gemeente-denhaag/paragraph";
import type { LiquidComponentDefinition } from "../components";

/**
 * A paragraph with the body as text.
 *
 * @example
 * {% paragraph %}Welkom bij de gemeente.{% endparagraph %}
 */
export const paragraphDefinition: LiquidComponentDefinition = {
  component: Paragraph,
  props: {},
  body: {},
};
