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
import LiquidTabs from "../../../components/liquid/LiquidTabs";
import type { LiquidTagDefinition } from "../tags";
import { valueLabelOptions } from "./filter";

/**
 * Tabs over a binding of a dynamic page, with `value | label` rows in the body; the tab whose value is the
 * binding's current `param` is selected. `binding`, `param` and `page-param` are names (quoted); `page-param`
 * is reset on change. The content below the tabs follows through the binding refetch.
 *
 * @example
 * {% tabs binding: "producten" param: "filter.status" page-param: "page" %}
 *  | Alle
 * actief | Actief
 * {% endtabs %}
 */
export const tabsTag: LiquidTagDefinition = {
  component: LiquidTabs,
  props: { binding: "binding", param: "param", "page-param": "pageParam" },
  body: { parse: (body) => ({ options: valueLabelOptions(body) }) },
};
