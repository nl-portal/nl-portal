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
import { useMatches } from "react-router";
import { getCurrentNavigationPage } from "../utils/get-current-navigation-page";
import RouterContext from "../contexts/RouterContext";

// TODO: heeft wat aandacht nodig, React heeft nieuwe api waar dit makkelijker kan
const PageMetaData = () => {
  const intl = useIntl();
  const matches = useMatches();
  const { navigationItems } = useContext(RouterContext);
  const currentPage =
    getCurrentNavigationPage(matches, navigationItems) || navigationItems[0][0];
  const pageTitle = intl.formatMessage({
    id: `pageTitles.${currentPage?.titleTranslationKey}`,
  });
  const appName = intl.formatMessage({ id: "app.appName" });
  const documentTitle = currentPage?.titleTranslationKey
    ? `${pageTitle} - ${appName}`
    : appName;

  useEffect(() => {
    document.title = documentTitle;
  }, [documentTitle]);

  return null;
};

export default PageMetaData;
