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
import {
  ApiContext,
  Bericht,
  GetBerichtDocument,
  GetVtbBerichtDocument,
  VtbBericht,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import { useOutletContext } from "react-router";
import BackLink from "../components/BackLink";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import { Paragraph } from "@gemeente-denhaag/typography";
import { FormattedDate, FormattedMessage, FormattedTime } from "react-intl";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { useContext, useEffect } from "react";
import AppContext from "../contexts/AppContext";
import MessageContent from "../components/MessageContent";
import DocumentsList from "../components/DocumentsList";
import useRequiredParams from "../hooks/useRequiredParams";

const MessageDetailsPage = () => {
  const { id } = useRequiredParams<{ id: string }>();
  const { features, refetchMessageCount } = useContext(AppContext);
  const { restUri } = useContext(ApiContext);
  const { paths } = useOutletContext<RouterOutletContext>();
  const berichtenEnabled = features?.modules.berichten?.enabled;
  const vtbBerichtenEnabled = features?.toggles.vtbBerichtenEnabled;
  const berichtenApi = vtbBerichtenEnabled ? "vtbberichten" : "berichten";
  const {
    data: berichtenData,
    loading: berichtenLoading,
    error: berichtenError,
  } = useQuery(GetBerichtDocument, {
    variables: { id },
    skip: !berichtenEnabled || vtbBerichtenEnabled,
  });
  const {
    data: vtbData,
    loading: vtbLoading,
    error: vtbError,
  } = useQuery(GetVtbBerichtDocument, {
    variables: { id },
    skip: !berichtenEnabled || !vtbBerichtenEnabled,
  });

  useEffect(() => {
    try {
      refetchMessageCount();
    } catch (error) {
      console.error("Error refetching messages:", error);
    }
  }, [berichtenData, vtbData]);

  const loading = berichtenLoading || vtbLoading;
  const error = berichtenError || vtbError;
  const message =
    (berichtenData?.getBericht as Bericht | undefined) ||
    (vtbData?.getVTBBericht as VtbBericht | undefined);

  return (
    <PageGrid variant="medium">
      <div>
        <BackLink href={paths.messages} />
        <PageHeader loading={loading} title={message?.onderwerp}>
          {message?.publicatiedatum && (
            <Paragraph>
              <FormattedMessage
                id="messageDetails.sent"
                values={{
                  date: (
                    <FormattedDate
                      value={message?.publicatiedatum}
                      year="numeric"
                      month="long"
                      day="numeric"
                    />
                  ),
                  time: (
                    <FormattedTime
                      value={message?.publicatiedatum}
                      hour="numeric"
                      minute="numeric"
                    />
                  ),
                }}
              />
            </Paragraph>
          )}
        </PageHeader>
      </div>
      <MessageContent
        loading={loading}
        error={Boolean(error)}
        messageText={message?.berichtTekst}
      />
      {!loading && (
        <DocumentsList
          loading={loading}
          error={Boolean(error)}
          documents={message?.documenten}
          showEmpty={false}
          getDownloadLink={(doc) =>
            `${restUri}/${berichtenApi}/${id}/document/${doc.uuid}/content`
          }
        />
      )}
    </PageGrid>
  );
};

export default MessageDetailsPage;
