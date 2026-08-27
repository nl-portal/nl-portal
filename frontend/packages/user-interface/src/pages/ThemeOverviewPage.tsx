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
import CasesList from "../components/CasesList";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import {
  OpenProductThema,
  GetOpenProductThemaTakenDocument,
  GetOpenProductThemaZakenDocument,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import TasksList from "../components/TasksList";
import { TaakV2, Zaak } from "@nl-portal/nl-portal-api";
import AppContext from "../contexts/AppContext";
import { use } from "react";
import { stringToSlug } from "../utils/string-to-slug";
import { ProductFetchList } from "../components/ProductFetchList";
import { ProductSettings } from "../interfaces/product-types";

interface Props {
  slug: string;
  fetchTasksLength?: number;
  fetchCasesLength?: number;
  productenSettings: ProductSettings[];
  children?: ((theme: OpenProductThema) => React.ReactNode) | React.ReactNode;
}

const ThemeOverviewPage = ({
  slug,
  fetchTasksLength = 5,
  fetchCasesLength = 4,
  productenSettings,
  children,
}: Props) => {
  const intl = useIntl();
  const { themes } = use(AppContext);
  const theme = themes.find(
    (theme) => stringToSlug(theme.naam) === stringToSlug(slug),
  ) as OpenProductThema;

  const { data: takenData, loading: takenLoading } = useQuery(
    GetOpenProductThemaTakenDocument,
    {
      variables: {
        id: theme?.uuid,
        pageSize: fetchTasksLength,
      },
    },
  );
  const { data: zakenData, loading: zakenLoading } = useQuery(
    GetOpenProductThemaZakenDocument,
    {
      variables: {
        id: theme?.uuid,
        pageSize: fetchCasesLength,
      },
    },
  );

  const loading = takenLoading || zakenLoading;
  const taken = (takenData?.getOpenProductThemaTaken as TaakV2[]) ?? [];
  const zaken = (zakenData?.getOpenProductThemaZaken as Zaak[]) ?? [];

  return (
    <PageGrid>
      <PageHeader title={intl.formatMessage({ id: `pageTitles.${slug}` })} />
      {Boolean(fetchTasksLength) && (
        <TasksList
          loading={loading}
          showEmpty={false}
          tasks={taken}
          openInContext={true}
        />
      )}
      {Boolean(fetchCasesLength) && (
        <CasesList
          loading={loading}
          showEmpty={false}
          listView={false}
          cases={zaken}
        />
      )}
      {productenSettings.map((productSettings) => (
        <ProductFetchList
          key={productSettings.titleTranslationId}
          slug={slug}
          productTypeSlug={productSettings.productTypeSlug}
          productLength={5}
          pagination={false}
          {...productSettings}
        />
      ))}
      {children instanceof Function ? children(theme) : children}
    </PageGrid>
  );
};

export default ThemeOverviewPage;
