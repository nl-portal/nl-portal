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
import { REGEX_PATTERNS } from "../../../constants/regex-patterns";
import TableList from "../../../components/TableList";
import type { CellObject } from "../../../components/Table";
import { decodeLiquidText } from "../decodeLiquidText";
import type { LiquidComponentDefinition } from "../components";

function tableCell(value: string, header = false): CellObject {
  let text = decodeLiquidText(value.trim().replace(/\\\|/g, "|"));
  const head = header || /^\*\*.*\*\*$/.test(text);
  if (text.startsWith("**") && text.endsWith("**")) text = text.slice(2, -2);
  const link = text.match(/^\[([^\]]*)\]\(([^\s]*)\)$/);
  if (!link) return { children: text, head };
  const href = link[2];
  if (
    REGEX_PATTERNS.urlProtocol.test(href) &&
    !REGEX_PATTERNS.externalLink.test(href)
  ) {
    throw new Error("Unsupported table link protocol.");
  }
  return { children: link[1], href, head };
}

function tableProps(body: string) {
  const lines = body.split(/\r?\n/).filter((line) => line.trim());
  const cells = lines.map((line) => {
    let row = line.trim();
    if (row.startsWith("|")) row = row.slice(1);
    if (/(?<!\\)\|$/.test(row)) row = row.slice(0, -1);
    return row.split(/(?<!\\)\|/);
  });
  const header = cells.shift() ?? [];
  // Accept the optional separator row used in standard Markdown tables.
  if (cells[0]?.every((cell) => /^:?-{3,}:?$/.test(cell.trim()))) cells.shift();
  if (cells.some((row) => row.length !== header.length)) {
    throw new Error(
      "Table rows must have the same number of cells as the header.",
    );
  }
  return {
    headers: header.map((cell) => tableCell(cell, true)),
    rows: cells.map((row) => row.map((cell) => tableCell(cell))),
  };
}

/**
 * TableList block: the first non-empty line contains the column headers.
 * Only `title` is allowed; it maps to `titleTranslationId` (a translation key).
 * The body supplies `headers` and `rows`, so those cannot be overridden as args.
 * Use `[text](/path)` for a cell link (`href`), `**text**` for a row header
 * (`head: true`), and `\|` for a literal pipe. `className` is not exposed.
 * Column headers always have `head: true`. Standard Markdown outer pipes and
 * a separator row are optional. Variables and filters work inside every cell.
 * A link in any body cell applies to every cell in that row, including its
 * arrow. If a row contains multiple links, the first link wins. Rows without
 * links stay unlinked. TableList adds an arrow column if any row has a link.
 *
 * @example
 * {% table title: "tableList.title" %}
 * Omschrijving | Bedrag
 * **[Aanvraag parkeervergunning](/parkeren/aanvragen)** | {{ product.objectdata.aanvraagkosten | currency }}
 * Wijzigen kenteken | [{{ product.objectdata.wijzigkosten | currency }}](/parkeren/wijzigen)
 * [Vervangende pas](/parkeren/vervangende-pas) | {{ product.objectdata.vervangkosten | currency }}
 * {% endtable %}
 */
export const tableDefinition: LiquidComponentDefinition = {
  component: TableList,
  props: { title: "titleTranslationId" },
  body: { parse: tableProps },
};
