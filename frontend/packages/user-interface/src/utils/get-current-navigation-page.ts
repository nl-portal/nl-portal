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
import { UIMatch } from "react-router";
import { NavigationItem } from "../interfaces/navigation-item";

/**
 * Needed for multiple backwards compatibility reasons, because the old nl-portal design
 * shows the current page + icon below the header. In case of the childpages, the parent page + icon is shown.
 * Also the document title show this "navigation page" (first parent which is shown in the menu)
 *
 * @param matches react-router useMatches
 * @param navigationItems navigationItems
 * @returns current navigation page
 */
export const getCurrentNavigationPage = (
  matches: UIMatch[],
  navigationItems: NavigationItem[][],
) => {
  return navigationItems
    .flat()
    .reverse()
    .find((item) => {
      return matches.find((match) => item?.path === match.pathname);
    });
};
