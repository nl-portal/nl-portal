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
import LiquidFilter from "../../../components/liquid/LiquidFilter";
import type { LiquidTagDefinition } from "../tags";
import { pipeRows } from "../text";

/** `value | label` rows; a row without `|` uses its value as label. */
export const valueLabelOptions = (body: string) =>
  pipeRows(body).map(({ left, right }) => ({
    value: left,
    label: right ?? left,
  }));

/**
 * A filter select over a binding of a dynamic page, with `value | label` rows in the body.
 * An empty value removes the override. `binding`, `param` and `page-param` are names (quoted);
 * `title` is a message id, shown as is when no message exists. `page-param` is reset on change.
 *
 * @example
 * {% filter binding: "vergunningen" param: "filter.status" title: "Status" page-param: "page" %}
 *  | Alle
 * open | Open
 * gesloten | Gesloten
 * {% endfilter %}
 */
export const filterTag: LiquidTagDefinition = {
  component: LiquidFilter,
  props: {
    binding: "binding",
    param: "param",
    title: "title",
    "page-param": "pageParam",
  },
  body: { parse: (body) => ({ options: valueLabelOptions(body) }) },
};
