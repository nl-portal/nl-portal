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
import { Zaak } from "@nl-portal/nl-portal-api";
import { CaseCard } from "@gemeente-denhaag/card";
import PortalLink from "./PortalLink";
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";

interface Props {
  cs: Zaak;
  listView?: boolean;
}

const Case = ({ cs, listView }: Props) => {
  const intl = useIntl();
  const { paths } = useOutletContext<RouterOutletContext>();
  const title = intl.formatMessage({
    id: `case.${cs.zaaktype.identificatie}.title`,
    defaultMessage: cs.zaaktype.omschrijving || "",
  });
  const appearance = listView
    ? "list"
    : cs.status?.statustype.isEindstatus
      ? "archived"
      : "default";

  return (
    <CaseCard
      appearance={appearance}
      title={title}
      subTitle={cs.omschrijving}
      href={paths.case(cs.uuid)}
      Link={PortalLink}
      context={cs.identificatie}
    />
  );
};

export default Case;
