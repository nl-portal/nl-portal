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
import PortalLink from "./PortalLink";
import { ActionSingle } from "@gemeente-denhaag/action";
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { FormattedMessage } from "react-intl";
import { DataBadge } from "@gemeente-denhaag/data-badge";
import styles from "./Message.module.scss";
import { Bericht } from "@nl-portal/nl-portal-api";
import { useContext } from "react";
import {
  LocaleContext,
  useActionLabels,
} from "@nl-portal/nl-portal-localization";

interface Props {
  message: Bericht;
}

const Message = ({ message }: Props) => {
  const labels = useActionLabels();
  const { paths } = useOutletContext<RouterOutletContext>();
  const { currentLocale } = useContext(LocaleContext);

  return (
    <ActionSingle
      labels={labels}
      dateTime={message.publicatiedatum}
      locale={currentLocale}
      link={paths.message(message.id ?? undefined)}
      Link={PortalLink}
    >
      {message.geopend ? (
        message.onderwerp
      ) : (
        <>
          <DataBadge status="neutral" className={styles["message__badge"]}>
            <FormattedMessage id="messagesList.new" />
          </DataBadge>
          <b>{message.onderwerp}</b>
        </>
      )}
    </ActionSingle>
  );
};

export default Message;
