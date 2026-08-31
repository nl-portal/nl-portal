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
import { useContext, useMemo } from "react";
import { QuickLinks as QuickLinksComponent } from "@gemeente-denhaag/quick-links";
import { useIntl } from "react-intl";
import RouterContext from "../contexts/RouterContext";
import PortalLink from "./PortalLink";
import styles from "./QuickLinks.module.scss";
import SectionHeader from "./SectionHeader";

interface QuickLinkProps {
  titleTranslationId?: string | null;
}

const QuickLinks = ({
  titleTranslationId = "quickLinks.title",
}: QuickLinkProps) => {
  const intl = useIntl();
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;
  const { navigationItems } = useContext(RouterContext);
  const items = useMemo(() => {
    return navigationItems
      .flat()
      .filter((item) => item.showInQuickLinks)
      .map((item) => ({
        Icon: item.icon,
        label: intl.formatMessage({
          id: `pageTitles.${item.titleTranslationKey}`,
        }),
        href: item.path,
      }));
  }, []);

  if (items.length === 0) return null;

  return (
    <section className={styles["quick-links"]}>
      <SectionHeader title={title} />
      <QuickLinksComponent items={items} Link={PortalLink} />
    </section>
  );
};

export default QuickLinks;
