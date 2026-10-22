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
import { useContext } from "react";
import { Navigate, Route, Routes, useOutletContext } from "react-router";
import AppContext from "../contexts/AppContext";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import {
  MENU_ENTRY_RENDERERS,
  MENU_ENTRY_SUB_PATH_TYPES,
} from "../utils/menu-entry-renderers";

/**
 * Renders the pages of the backend-configured menu entries. Mount it as the element of the app's "*" route.
 * Any path that isn't a menu entry redirects to `paths.noMatch`.
 */
const MenuEntryRoutes = () => {
  const { menuEntries } = useContext(AppContext);
  const { paths } = useOutletContext<RouterOutletContext>();

  return (
    <Routes>
      {menuEntries.map((entry) => {
        const Renderer = MENU_ENTRY_RENDERERS[entry.route.type];
        const path = MENU_ENTRY_SUB_PATH_TYPES.includes(entry.route.type)
          ? `${entry.route.path}/*`
          : entry.route.path;
        return (
          <Route
            key={entry.route.path}
            path={path}
            element={<Renderer entry={entry} />}
          />
        );
      })}
      <Route path="*" element={<Navigate to={paths.noMatch} />} />
    </Routes>
  );
};

export default MenuEntryRoutes;
