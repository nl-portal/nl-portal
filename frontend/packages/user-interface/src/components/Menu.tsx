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
import { Link, useMatches } from "react-router";
import { FormattedMessage } from "react-intl";
import { LocaleContext } from "@nl-portal/nl-portal-localization";
import {
  SideNavigationBase,
  SideNavigationItem,
  SideNavigationLinkLabel,
  SideNavigationList,
} from "@gemeente-denhaag/side-navigation";
import { getCurrentNavigationPage } from "../utils/get-current-navigation-page";
import { NumberBadge } from "@gemeente-denhaag/number-badge";
import AppContext from "../contexts/AppContext";
import RouterContext from "../contexts/RouterContext";

const Menu = () => {
  const { hrefLang } = useContext(LocaleContext);
  const { messagesCount } = useContext(AppContext);
  const { navigationItems } = useContext(RouterContext);
  const matches = useMatches();
  const currentNavigationItem = getCurrentNavigationPage(
    matches,
    navigationItems,
  );

  return (
    <SideNavigationBase>
      {navigationItems.map((array, index) => (
        <SideNavigationList key={`sidenav-list-${index}`}>
          {array.map((item) => {
            const Icon = item.icon;
            const current = item === currentNavigationItem;
            const className = `denhaag-side-navigation__link ${
              current && "denhaag-side-navigation__link--current"
            }`;

            return (
              <SideNavigationItem key={item.path}>
                <Link className={className} hrefLang={hrefLang} to={item.path}>
                  <Icon />
                  <SideNavigationLinkLabel>
                    <FormattedMessage
                      id={`pageTitles.${item.titleTranslationKey}`}
                    />
                    {item.hasMessagesCount && messagesCount > 0 && (
                      <NumberBadge>{messagesCount}</NumberBadge>
                    )}
                  </SideNavigationLinkLabel>
                </Link>
              </SideNavigationItem>
            );
          })}
        </SideNavigationList>
      ))}
    </SideNavigationBase>
  );
};

export default Menu;
