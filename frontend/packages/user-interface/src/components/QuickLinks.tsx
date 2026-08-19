import { useContext, useMemo } from "react";
import { QuickLinks as QuickLinksComponent } from "@gemeente-denhaag/quick-links";
import { useIntl } from "react-intl";
import RouterContext from "../contexts/RouterContext";
import PortalLink from "./PortalLink";
import styles from "./QuickLinks.module.scss";
import classNames from "classnames";

const QuickLinks = () => {
  const intl = useIntl();
  const { navigationItems } = useContext(RouterContext);
  const classes = classNames(
    "denhaag-quick-links",
    styles["denhaag-quick-links--mobile"],
  );
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

  console.log(classes);

  return (
    <QuickLinksComponent className={classes} items={items} Link={PortalLink} />
  );
};

export default QuickLinks;
