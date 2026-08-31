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
import { render, waitFor, screen } from "@testing-library/react";
import { MockOverviewPage } from "../mock/pages/OverviewPage.mock";

describe("Page", () => {
  const openZaak1 = () => screen.getByText("case.OPENZAAK1.title");

  it("should correctly set the document title", async () => {
    render(MockOverviewPage());

    await waitFor(() => {
      expect(openZaak1()).toBeVisible();
    });

    expect(document.title).toContain("Overzicht - NL Portal");
  });
});
