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
import styles from "./SectionHeader.module.scss";
import PortalLink from "./PortalLink";
import { Link } from "@gemeente-denhaag/link";
import classnames from "classnames";
import { ArrowRightIcon } from "@gemeente-denhaag/icons";
import Heading from "./Heading";

interface Props {
  title?: string;
  small?: boolean;
  href?: string;
  subTitle?: string;
}

const SectionHeader = ({ title, small, href, subTitle }: Props) => {
  if (!title) return null;

  return (
    <header
      className={classnames(styles["section-header"], {
        [styles["section-header--small"]]: small,
      })}
    >
      {small ? (
        <Heading as="h4">{title}</Heading>
      ) : (
        <Heading as="h3">{title}</Heading>
      )}
      {href && subTitle && (
        <Link
          className={styles["section-header__link"]}
          href={href}
          Link={PortalLink}
          icon={<ArrowRightIcon />}
        >
          {subTitle}
        </Link>
      )}
    </header>
  );
};

export default SectionHeader;
