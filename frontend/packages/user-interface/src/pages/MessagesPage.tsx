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
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import MessagesList from "../components/MessagesList";
import {
  Bericht,
  GetBerichtenDocument,
  GetVtbBerichtenDocument,
  VtbBericht,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import SearchForm from "../components/SearchForm";
import AppContext from "../contexts/AppContext";
import { useContext } from "react";

const MessagesPage = () => {
  const intl = useIntl();
  const { features } = useContext(AppContext);
  const vtbBerichtenEnabled = features?.toggles.vtbBerichtenEnabled;
  const {
    data: berichtenData,
    loading: berichtenLoading,
    error: berichtenError,
    refetch: refetchBerichten,
  } = useQuery(GetBerichtenDocument, {
    skip: vtbBerichtenEnabled,
    variables: { pageSize: 10 },
    fetchPolicy: "cache-and-network",
  });
  const {
    data: vtbData,
    loading: vtbLoading,
    error: vtbError,
    refetch: refetchVtbMessages,
  } = useQuery(GetVtbBerichtenDocument, {
    skip: !vtbBerichtenEnabled,
    variables: { pageSize: 10 },
    fetchPolicy: "cache-and-network",
  });

  const loading = berichtenLoading || vtbLoading;
  const error = berichtenError || vtbError;
  const refetch = berichtenData ? refetchBerichten : refetchVtbMessages;
  const messages =
    (berichtenData?.getBerichten.content as Bericht[]) ||
    (vtbData?.getVTBBerichten?.content as VtbBericht[]) ||
    undefined;
  const totalElements =
    berichtenData?.getBerichten.totalElements ||
    vtbData?.getVTBBerichten?.totalElements ||
    0;
  const totalPages =
    berichtenData?.getBerichten.totalPages ||
    vtbData?.getVTBBerichten?.totalPages ||
    0;

  const handleFormSubmit = (searchValue: string) => {
    try {
      refetch({ onderwerp: searchValue });
    } catch (error) {
      console.error("Error fetching messages:", error);
    }
  };

  const onPageChange = (index: number) => {
    try {
      refetch({ pageNumber: index + 1 });
    } catch (error) {
      console.error("Error fetching messages:", error);
    }
  };

  return (
    <PageGrid variant="medium">
      <PageHeader
        title={intl.formatMessage({ id: "pageTitles.messages" })}
      ></PageHeader>
      {!Boolean(vtbBerichtenEnabled) && (
        <SearchForm
          translationId="messages"
          totalElements={totalElements}
          onSubmit={handleFormSubmit}
        />
      )}
      <MessagesList
        loading={loading}
        error={Boolean(error)}
        messages={messages}
        indexLimit={totalPages && totalPages - 1}
        onChange={onPageChange}
      />
    </PageGrid>
  );
};

export default MessagesPage;
