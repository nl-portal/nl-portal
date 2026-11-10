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
import { GetTakenV2Document, TaakV2 } from "@nl-portal/nl-portal-api";
import TasksList from "../../../components/TasksList.tsx";
import { useQuery } from "@apollo/client/react";
import { FormattedMessage } from "react-intl";
import PageHeader from "../../../components/PageHeader.tsx";

const LiquidMijnTaken = ({ thema = "" }: { thema: string; }) => {
  console.log(`THEMA! ${thema}`);
  const { data, loading, error, refetch } = useQuery(GetTakenV2Document);
  const tasks = data?.getTakenV2.content as TaakV2[] | undefined;

  const onPageChange = (index: number) => {
    try {
      refetch({ pageNumber: index + 1 });
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  };

  return (
    <div>
      <PageHeader
        title={<FormattedMessage id="pageTitles.tasks" />}
      ></PageHeader>
      <TasksList
        loading={loading}
        error={Boolean(error)}
        titleTranslationId={null}
        tasks={tasks}
        onChange={onPageChange}
        openInContext={true}
      />
    </div>
  );
};

export const mijnTakenDefinition: LiquidComponentDefinition = {
  component: LiquidMijnTaken,
  props: {
    title: "titleTranslationId",
    binding: "binding",
    "page-param": "pageParam",
    "page-size": "pageSize",
    total: "total",
    thema: "thema"
  },
};
