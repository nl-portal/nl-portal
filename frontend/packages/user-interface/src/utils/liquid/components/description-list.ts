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
import DescriptionList from "../../../components/DescriptionList";
import { decodeLiquidText } from "../decodeLiquidText";
import type { LiquidComponentDefinition } from "../components";

function descriptionItems(body: string) {
  return body
    .split(/\r?\n/)
    .filter((line) => line.trim())
    .map((line) => {
      const separator = line.search(/(?<!\\)\|/);
      if (separator === -1)
        throw new Error(
          "Description-list rows must contain a title | detail separator.",
        );
      const text = (value: string) =>
        decodeLiquidText(value.trim().replace(/\\\|/g, "|"));
      return {
        title: text(line.slice(0, separator)),
        detail: text(line.slice(separator + 1)),
      };
    });
}

/**
 * Liquid block with Markdown-style `label | value` rows.
 *
 * Only `title` is allowed as a template prop. It maps to DescriptionList's
 * `titleTranslationId`, so its value should be a translation key. Omitting it
 * uses the component's default key, `descriptionList.title`.
 *
 * The rendered body becomes the `items` prop: each non-empty line is split at
 * the first unescaped `|` into an item's `title` and `detail`. Whitespace is
 * trimmed, blank lines are ignored, and a missing separator is a parse error.
 * Write `\|` for a literal pipe. Liquid variables and filters are evaluated
 * before the rows are parsed; escaped entities are decoded back into text.
 *
 * @example
 * {% description-list title: "descriptionList.title" %}
 * Naam | {{ naam | escape }}
 * Startdatum | {{ startDatum | date | escape }}
 * Status | {{ status | capitalize | escape }}
 * Prijs | {{ prijs | currency | escape }}
 * {% enddescription-list %}
 */
export const descriptionListDefinition: LiquidComponentDefinition = {
  component: DescriptionList,
  props: {
    title: "titleTranslationId",
  },
  body: { parse: (body) => ({ items: descriptionItems(body) }) },
};
