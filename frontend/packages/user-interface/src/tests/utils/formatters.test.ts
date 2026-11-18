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
import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { createIntl } from "react-intl";
import { Liquid } from "liquidjs";
import { createLiquidEngine } from "../../utils/liquid/engine";
import { PORTAL_FILTERS } from "../../utils/liquid/formatters";

const intl = createIntl({ locale: "nl", onError: () => {} });

const pagesDoc = readFileSync(
  resolve(
    __dirname,
    "../../../../../../documentation/features/portal-pages.md",
  ),
  "utf8",
);

const portalFiltersSection = () => {
  const start = pagesDoc.indexOf("\n## Portaalfilters\n");
  const end = pagesDoc.indexOf("\n## ", start + 1);
  return pagesDoc.slice(start, end === -1 ? undefined : end);
};

describe("PORTAL_FILTERS", () => {
  it.each(PORTAL_FILTERS)("registers %s on the engine", (name) => {
    const engine = createLiquidEngine(intl);
    const builtIn = new Liquid().filters[name];

    expect(engine.filters[name]).toBeDefined();
    expect(engine.filters[name]).not.toBe(builtIn);
  });

  it.each(PORTAL_FILTERS)(
    "documents %s under Portaalfilters in portal-pages.md",
    (name) => {
      expect(portalFiltersSection()).toContain(`\n### \`${name}\`\n`);
    },
  );
});
