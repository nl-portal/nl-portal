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
 * Whether the navigation item is the current page for the pathname: an exact path match,
 * or for items with `matchSubPaths` also every pathname below the item's path.
 */
export const isCurrentFor = (item: NavigationItem, pathname: string) =>
  item.path === pathname ||
  (!!item.matchSubPaths && pathname.startsWith(`${item.path}/`));

/**
 * Needed for multiple backwards compatibility reasons, because the old nl-portal design
 * shows the current page + icon below the header. In case of the childpages, the parent page + icon is shown.
 * Also the document title show this "navigation page" (first parent which is shown in the menu)
 *
 * The deepest match that has a navigation item wins.
 *
 * @param matches react-router useMatches
 * @param navigationItems navigationItems
 * @returns current navigation page
 */
export const getCurrentNavigationPage = (
  matches: UIMatch[],
  navigationItems: NavigationItem[][],
) => {
  const items = navigationItems.flat();

  for (const match of [...matches].reverse()) {
    const item = items.find((item) => isCurrentFor(item, match.pathname));
    if (item) return item;
  }

  return undefined;
};

/**
 * Returns the navigation item for the deepest match when that match has no breadcrumb label,
 * such as a menu entry page rendered under the app's "*" route.
 *
 * @param matches react-router useMatches
 * @param navigationItems navigationItems
 * @returns navigation item for the unlabeled deepest match
 */
export const getUnlabeledNavigationPage = (
  matches: UIMatch<unknown, { label?: string } | undefined>[],
  navigationItems: NavigationItem[][],
) => {
  const deepestMatch = matches[matches.length - 1];
  if (!deepestMatch || deepestMatch.handle?.label) return undefined;

  return navigationItems
    .flat()
    .find((item) => isCurrentFor(item, deepestMatch.pathname));
};
