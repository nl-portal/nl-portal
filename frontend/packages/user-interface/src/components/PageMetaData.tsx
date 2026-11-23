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
import { useContext, useEffect } from "react";
import { useIntl } from "react-intl";
import { UIMatch, useMatches } from "react-router";
import {
  getCurrentNavigationPage,
  getHiddenMenuEntryPage,
} from "../utils/get-current-navigation-page";
import AppContext from "../contexts/AppContext";
import RouterContext from "../contexts/RouterContext";
import { getNavigationItemTitleId } from "../utils/navigation-item-title";

// TODO: heeft wat aandacht nodig, React heeft nieuwe api waar dit makkelijker kan
const PageMetaData = () => {
  const intl = useIntl();
  const matches = useMatches() as UIMatch<unknown, { label?: string }>[];
  const { navigationItems } = useContext(RouterContext);
  const { menuEntries } = useContext(AppContext);
  const appName = intl.formatMessage({ id: "app.appName" });
  const hiddenEntry = getHiddenMenuEntryPage(
    matches,
    navigationItems,
    menuEntries ?? [],
  );
  const hiddenTitleId = hiddenEntry && `menu-entries.${hiddenEntry.name}`;
  const currentPage = hiddenEntry
    ? undefined
    : getCurrentNavigationPage(matches, navigationItems) ||
      navigationItems[0][0];
  const pageTitle = hiddenEntry
    ? hiddenTitleId &&
      intl.messages[hiddenTitleId] &&
      intl.formatMessage({ id: hiddenTitleId })
    : currentPage?.titleTranslationKey &&
      intl.formatMessage({ id: getNavigationItemTitleId(currentPage) });
  const documentTitle = pageTitle ? `${pageTitle} - ${appName}` : appName;

  useEffect(() => {
    document.title = documentTitle;
  }, [documentTitle]);

  return null;
};

export default PageMetaData;
