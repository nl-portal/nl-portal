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
import { useIntl } from "react-intl";
import Skeleton from "./Skeleton";
import SectionHeader from "./SectionHeader";
import { Link, LinkProps } from "@gemeente-denhaag/link";
import { LinkListBase, LinkListItem } from "@gemeente-denhaag/link-list";
import PortalLink from "./PortalLink";
import { listViewHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  titleTranslationId?: string | null;
  links?: LinkProps[];
}

const LinkList = ({
  loading,
  titleTranslationId = "linkList.title",
  links,
}: Props) => {
  const intl = useIntl();
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;

  if (loading) {
    return (
      <section>
        <SectionHeader title={title} />
        <Skeleton height={listViewHeight} />
        <Skeleton height={listViewHeight} />
        <Skeleton height={listViewHeight} />
      </section>
    );
  }

  if (!links || links.length === 0) {
    return null;
  }

  return (
    <section>
      <SectionHeader title={title} />
      <LinkListBase>
        {links.map((link, index) => (
          <LinkListItem key={index}>
            <Link {...link} Link={PortalLink} />
          </LinkListItem>
        ))}
      </LinkListBase>
    </section>
  );
};

export default LinkList;
