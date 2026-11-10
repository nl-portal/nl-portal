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
import { useQuery } from "@apollo/client/react";
import {
  GetOpenProductThemaZakenByNameDocument,
  type Zaak,
} from "@nl-portal/nl-portal-api";
import CasesList from "../../../components/CasesList.tsx";
import type { LiquidComponentDefinition } from "../components";

const LiquidThemeCases = ({
  theme = "",
  pageSize = 5,
}: {
  theme: string;
  pageSize: number;
}) => {
  const { data, loading, error } = useQuery(
    GetOpenProductThemaZakenByNameDocument,
    {
      variables: { naam: theme, pageSize },
    },
  );
  const cases = data?.getOpenProductThemaZakenByName as Zaak[] | undefined;

  return <CasesList loading={loading} error={Boolean(error)} cases={cases} />;
};

export const themeCasesDefinition: LiquidComponentDefinition = {
  component: LiquidThemeCases,
  props: {
    theme: "theme",
    "page-size": "pageSize",
  },
};
