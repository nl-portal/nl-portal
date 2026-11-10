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
import type { LiquidComponentDefinition } from "../components";
import { useQuery } from "@apollo/client/react";
import { FormattedMessage } from "react-intl";
import PageHeader from "../../../components/PageHeader.tsx";
import CasesList from "../../../components/CasesList.tsx";
import { useState, useTransition } from "react";
import { Zaak, GetZakenDocument } from "@nl-portal/nl-portal-api";

const LiquidMijnZaken = ({ thema = "" }: { thema: string; }) => {
  console.log(thema);
  const fetchCasesLength = 10;
  const [currentTab] = useState(0);
  const [a, setOpenIndex] = useState(0);
  const [closedIndex, setClosedIndex] = useState(0);
  const [isPending, startTransition] = useTransition();
  console.log(a);
  const {
    refetch: openRefetch,
  } = useQuery(GetZakenDocument, {
    variables: {
      isOpen: true,
      pageSize: fetchCasesLength,
    },
  });

  const {
    data: closedData,
    loading: closedLoading,
    error: closedError,
    refetch: closedRefetch,
  } = useQuery(GetZakenDocument, {
    variables: {
      isOpen: false,
      pageSize: fetchCasesLength,
    },
  });

  const closedCases = closedData?.getZaken.content as Zaak[] | undefined;


  const onPageChange = (index: number) => {
    startTransition(async () => {
      try {
        const func = currentTab === 0 ? openRefetch : closedRefetch;
        if (currentTab === 0) setOpenIndex(index);
        if (currentTab === 1) setClosedIndex(index);
        await func({
          page: index + 1,
        });
      } catch (error) {
        console.error("Error fetching cases:", error);
      }
    });
  };

  return (
    <div>
      <PageHeader
        title={<FormattedMessage id="pageTitles.cases" />}
      ></PageHeader>
      <CasesList
        loading={isPending || closedLoading}
        error={Boolean(closedError)}
        titleTranslationId={null}
        cases={closedCases}
        totalAmount={closedData?.getZaken.totalElements}
        index={closedIndex}
        indexLimit={
          !isPending && closedData?.getZaken.totalPages
            ? closedData?.getZaken.totalPages - 1
            : undefined
        }
        onChange={onPageChange}
      />
    </div>
  );
};

export const mijnZakenDefinition: LiquidComponentDefinition = {
  component: LiquidMijnZaken,
  props: {
    title: "titleTranslationId",
    binding: "binding",
    "page-param": "pageParam",
    "page-size": "pageSize",
    total: "total",
    thema: "thema",
  },
};
