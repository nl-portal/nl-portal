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
import { Liquid } from "liquidjs";
import type { IntlShape } from "react-intl";
import { registerTags } from "./tags";
import { registerFormatters } from "./formatters";
import { storeValue } from "./values";

/**
 * A Liquid engine for portal templates: strict variables and filters, own properties only,
 * bounded parse, render and memory use, the portal formatters and the component tags.
 * Every `{{ }}` output that doesn't end in `raw` becomes a value placeholder (see `values.ts`); `echo` and `cycle`
 * write their value as is. Create one engine per render.
 */
export function createLiquidEngine(intl: IntlShape) {
  const liquid: Liquid = new Liquid({
    strictVariables: true,
    strictFilters: true,
    ownPropertyOnly: true,
    parseLimit: 1e6,
    renderLimit: 1000,
    memoryLimit: 1e7,
    outputEscape: (value: unknown): string => storeValue(liquid, value),
  });

  registerFormatters(liquid, intl);
  registerTags(liquid);
  return liquid;
}
