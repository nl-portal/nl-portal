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
import BackLink from "../components/BackLink";
import {
  OpenProductProduct,
  TaakV2,
  GetOpenProductDocument,
  Zaak,
  GetOpenProductQueryVariables,
  GetOpenProductQuery,
} from "@nl-portal/nl-portal-api";
import { skipToken, useQuery } from "@apollo/client/react";
import TasksList from "../components/TasksList";
import CasesList from "../components/CasesList";
import DescriptionList from "../components/DescriptionList";
import React from "react";
import { DataMappingItem } from "../interfaces/product-types";
import { getProductValue } from "../utils/get-product-value";
import ProductDecisionsList from "../components/ProductDecisionList";
import useRequiredParams from "../hooks/useRequiredParams";

interface ThemeDetailsPageProps {
  children?:
    | ((
        openProduct: ReturnType<
          typeof useQuery<GetOpenProductQuery, GetOpenProductQueryVariables>
        >,
      ) => React.ReactNode)
    | React.ReactNode;
  productSettings?: {
    headerTranslationIds: string[];
    dataMapping: DataMappingItem[];
  };
}

const ThemeDetailsPage = ({
  children,
  productSettings,
}: ThemeDetailsPageProps) => {
  const intl = useIntl();
  const { id } = useRequiredParams<{ id: string }>();
  const openProduct = useQuery(
    GetOpenProductDocument,
    id
      ? {
          variables: { id },
        }
      : skipToken,
  );

  const { data, loading, error } = openProduct;
  const product = data?.getOpenProduct as OpenProductProduct | undefined;
  const zaken = product?.zaken as Zaak[] | undefined;
  const taken = product?.taken as TaakV2[] | undefined;
  const details =
    product && productSettings
      ? productSettings?.headerTranslationIds
          ?.map((header, index) => ({
            title: intl.formatMessage({ id: header }),
            detail: getProductValue(
              product,
              productSettings.dataMapping[index],
            ),
          }))
          .filter(({ detail }) => detail)
      : [];

  return (
    <PageGrid>
      <div>
        <BackLink />
        <PageHeader loading={loading} title={product?.naam} />
      </div>
      <TasksList
        loading={loading}
        error={Boolean(error)}
        showEmpty={false}
        titleTranslationId={null}
        tasks={taken}
      />
      <ProductDecisionsList
        loading={loading}
        productId={id}
        decisions={product?.decisions}
      />
      <CasesList
        loading={loading}
        error={Boolean(error)}
        showEmpty={false}
        listView={false}
        cases={zaken}
      />
      <DescriptionList loading={loading} showEmpty={false} items={details} />
      {children instanceof Function ? children(openProduct) : children}
      <TasksList
        loading={loading}
        showEmpty={false}
        error={Boolean(error)}
        titleTranslationId={null}
        tasks={taken}
      />
    </PageGrid>
  );
};

export default ThemeDetailsPage;
