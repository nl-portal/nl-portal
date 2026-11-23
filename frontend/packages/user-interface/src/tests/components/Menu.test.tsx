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
import { ContextType } from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { createMemoryRouter, RouterProvider } from "react-router";
import { DocumentIcon, GridIcon } from "@gemeente-denhaag/icons";
import Menu from "../../components/Menu";
import AppContext from "../../contexts/AppContext";
import RouterContext from "../../contexts/RouterContext";
import { MenuEntry } from "../../utils/menu-entries";

const hidden: MenuEntry = {
  name: "erfpacht-product",
  icon: "DocumentIcon",
  hidden: true,
  route: { type: "portal-page", page: "product", path: "/erfpacht/:id" },
};

const renderAt = (path: string, menuEntries: MenuEntry[]) => {
  const router = createMemoryRouter(
    [
      {
        path: "/",
        handle: { label: "breadcrumb.overview" },
        element: (
          <IntlProvider locale="nl" messages={{}} onError={() => {}}>
            <RouterContext.Provider
              value={
                {
                  navigationItems: [
                    [
                      {
                        titleTranslationKey: "overview",
                        titleTranslationId: "Overzicht",
                        path: "/",
                        icon: GridIcon,
                      },
                      {
                        titleTranslationKey: "erfpacht",
                        titleTranslationId: "Erfpacht",
                        path: "/erfpacht",
                        icon: DocumentIcon,
                      },
                    ],
                  ],
                } as unknown as ContextType<typeof RouterContext>
              }
            >
              <AppContext.Provider
                value={
                  { menuEntries, messagesCount: 0 } as unknown as ContextType<
                    typeof AppContext
                  >
                }
              >
                <Menu />
              </AppContext.Provider>
            </RouterContext.Provider>
          </IntlProvider>
        ),
        children: [{ path: "*", element: null }],
      },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
};

const isCurrent = (label: string) =>
  screen
    .getByText(label)
    .closest("a")!
    .classList.contains("denhaag-side-navigation__link--current");

describe("Menu", () => {
  it("highlights the visible entry above a hidden entry route", () => {
    renderAt("/erfpacht/abc", [hidden]);

    expect(isCurrent("Erfpacht")).toBe(true);
    expect(isCurrent("Overzicht")).toBe(false);
  });

  it("highlights overview on an unknown sub path", () => {
    renderAt("/erfpacht/abc/def", [hidden]);

    expect(isCurrent("Erfpacht")).toBe(false);
    expect(isCurrent("Overzicht")).toBe(true);
  });
});
