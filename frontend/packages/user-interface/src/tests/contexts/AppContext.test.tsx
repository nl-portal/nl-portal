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
import { useContext } from "react";
import { render, screen } from "@testing-library/react";
import AppContext from "../../contexts/AppContext";
import TestProvider, { testPaths } from "../../providers/TestProvider";
import {
  MENU_ENTRY_RENDERERS,
  MenuEntryRenderers,
} from "../../utils/menu-entry-renderers";

const RendererTypes = () => {
  const { menuEntryRenderers } = useContext(AppContext);
  return (
    <div data-testid="types">
      {Object.keys(menuEntryRenderers).sort().join(",")}
    </div>
  );
};

const renderWith = (menuEntryRenderers?: MenuEntryRenderers) =>
  render(
    <TestProvider
      mocks={[]}
      routes={[{ path: testPaths.overview, element: <RendererTypes /> }]}
      initialEntries={[testPaths.overview]}
      menuEntryRenderers={menuEntryRenderers}
    />,
  );

describe("AppProvider", () => {
  it("provides the default menu entry renderers", async () => {
    renderWith();

    expect(await screen.findByTestId("types")).toHaveTextContent(
      "custom,portal-page",
    );
  });

  it("provides the menu entry renderers of the implementation", async () => {
    renderWith({ ...MENU_ENTRY_RENDERERS, foo: () => <div /> });

    expect(await screen.findByTestId("types")).toHaveTextContent(
      "custom,foo,portal-page",
    );
  });
});
