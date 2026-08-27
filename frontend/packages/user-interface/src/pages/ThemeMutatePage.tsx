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
import BackLink from "../components/BackLink";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import { useParams } from "react-router";
import {
  GetOpenProductDocument,
  GetOpenProductQuery,
  GetOpenProductQueryVariables,
} from "@nl-portal/nl-portal-api";
import { skipToken, useQuery } from "@apollo/client/react";

interface Props {
  slug: string;
  titleTranslationId?: string;
  children?:
    | ((
        openProduct: ReturnType<
          typeof useQuery<GetOpenProductQuery, GetOpenProductQueryVariables>
        >,
      ) => React.ReactNode)
    | React.ReactNode;
}

const ThemeMutatePage = ({
  slug,
  titleTranslationId = `pageTitles.${slug}Mutate`,
  children,
}: Props) => {
  const intl = useIntl();
  const params = useParams<{ id: string }>();
  const openProduct = useQuery(
    GetOpenProductDocument,
    params.id
      ? {
          variables: { id: params.id },
        }
      : skipToken,
  );

  return (
    <PageGrid>
      <div>
        <BackLink />
        <PageHeader title={intl.formatMessage({ id: titleTranslationId })} />
      </div>
      {children instanceof Function ? children(openProduct) : children}
    </PageGrid>
  );
};

export default ThemeMutatePage;
