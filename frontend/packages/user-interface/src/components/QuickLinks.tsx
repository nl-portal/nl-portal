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

  return (
    <section className={styles["quick-links"]}>
      <SectionHeader title={title} />
      <QuickLinksComponent items={items} Link={PortalLink} />
    </section>
  );
};

export default QuickLinks;
