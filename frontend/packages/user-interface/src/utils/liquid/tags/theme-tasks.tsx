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
import type { LiquidTagDefinition } from "../tags";
import {
  GetOpenProductThemaTakenByNameDocument,
  TaakV2,
} from "@nl-portal/nl-portal-api";
import TasksList from "../../../components/TasksList";
import { useQuery } from "@apollo/client/react";

const LiquidThemeTasks = ({
  theme = "",
  pageSize = 5,
}: {
  theme: string;
  pageSize: number;
}) => {
  const { data, loading, error } = useQuery(
    GetOpenProductThemaTakenByNameDocument,
    {
      variables: {
        naam: theme,
        pageSize: pageSize,
      },
    },
  );
  const tasks = data?.getOpenProductThemaTakenByName as TaakV2[] | undefined;

  return (
    <TasksList
      loading={loading}
      error={Boolean(error)}
      tasks={tasks}
      openInContext={true}
    />
  );
};

/**
 * Widget tag: the user's tasks of the Open Product theme named `theme`, from the theme feature's
 * `getOpenProductThemaTakenByName` query. Not a binding, so no projection applies. Without a `theme` it shows an empty list;
 * `page-size` (default 5) is checked by the query, which only accepts 1 to 100.
 *
 * @example
 * {% theme-tasks theme: "Wonen en bouwen" page-size: 5 %}
 */
export const themeTasksTag: LiquidTagDefinition = {
  component: LiquidThemeTasks,
  props: {
    theme: "theme",
    "page-size": "pageSize",
  },
};
