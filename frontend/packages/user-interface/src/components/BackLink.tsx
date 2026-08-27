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
import React, { useContext } from "react";
import { FormattedMessage } from "react-intl";
import { LocaleContext } from "@nl-portal/nl-portal-localization";
import { Link } from "@gemeente-denhaag/link";
import { ChevronLeftIcon } from "@gemeente-denhaag/icons";
import styles from "./BackLink.module.scss";
import PortalLink from "./PortalLink";
import AppContext from "../contexts/AppContext";

interface Props {
  href?: string;
  children?: React.ReactNode;
}

export const BackLink = ({ href, children }: Props) => {
  const { hrefLang } = useContext(LocaleContext);
  const { history } = useContext(AppContext);

  if (!href && !history[1]) return null;

  return (
    <div className={styles["back-link"]}>
      <Link
        Link={PortalLink}
        href={href || history[1]}
        icon={<ChevronLeftIcon />}
        iconAlign="start"
        hrefLang={hrefLang}
      >
        {children || <FormattedMessage id={`backlink.back`} />}
      </Link>
    </div>
  );
};

export default BackLink;
