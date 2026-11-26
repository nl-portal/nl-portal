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
import {
  createMemoryRouter,
  Outlet,
  RouterProvider,
  useLocation,
} from "react-router";
import AppContext from "../../contexts/AppContext";
import { NotificationProvider } from "../../contexts/NotificationContext";
import MenuEntryRoutes from "../../components/MenuEntryRoutes";
import { MenuEntry } from "../../utils/menu-entries";
import { testPaths } from "../../providers/TestProvider";

const menuEntries: MenuEntry[] = [
  {
    name: "thema-concept",
    icon: "ParkingIcon",
    route: { type: "templated", page: "template-a", path: "/thema-concept" },
  },
  {
    name: "extern-portaal",
    icon: "GridIcon",
    route: { type: "custom", page: "remote-a", path: "/extern" },
  },
];

const Location = () => (
  <div data-testid="location">{useLocation().pathname}</div>
);

const renderAt = (path: string) => {
  const router = createMemoryRouter(
    [
      {
        element: (
          <IntlProvider locale="nl" onError={() => {}}>
            <NotificationProvider>
              <AppContext.Provider
                value={{ menuEntries } as ContextType<typeof AppContext>}
              >
                <Location />
                <Outlet context={{ paths: testPaths }} />
              </AppContext.Provider>
            </NotificationProvider>
          </IntlProvider>
        ),
        children: [
          { path: testPaths.noMatch, element: <div>not found route</div> },
          { path: "*", element: <MenuEntryRoutes /> },
        ],
      },
    ],
    { initialEntries: [path] },
  );

  render(<RouterProvider router={router} />);
};

describe("MenuEntryRoutes", () => {
  it("renders the placeholder for a templated entry", async () => {
    renderAt("/thema-concept");

    expect(await screen.findByText("noMatchPage.title")).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/thema-concept");
  });

  it("renders the placeholder for a custom entry sub path", async () => {
    renderAt("/extern/a/b");

    expect(await screen.findByText("noMatchPage.title")).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent("/extern/a/b");
  });

  it("navigates to noMatch for a templated entry sub path", async () => {
    renderAt("/thema-concept/a");

    expect(await screen.findByText("not found route")).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(testPaths.noMatch);
  });

  it("navigates to noMatch for an unknown path", async () => {
    renderAt("/bestaat-niet");

    expect(await screen.findByText("not found route")).toBeInTheDocument();
    expect(screen.getByTestId("location")).toHaveTextContent(testPaths.noMatch);
  });
});
