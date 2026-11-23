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
import type { ReactNode } from "react";
import { useIntl } from "react-intl";
import PageHeader from "../../../components/PageHeader";
import type { LiquidTagDefinition } from "../tags";

const LiquidPageHeader = ({
  title,
  subTitle,
  children,
}: {
  title?: string;
  subTitle?: string;
  children?: ReactNode;
}) => {
  const intl = useIntl();
  const text = (id?: string) =>
    id ? intl.formatMessage({ id, defaultMessage: id }) : undefined;
  return (
    <PageHeader title={text(title)} subTitle={text(subTitle)}>
      {children}
    </PageHeader>
  );
};

/**
 * The page header, with the body below the titles. `title` and `sub-title` are message ids,
 * shown as is when no message exists.
 *
 * @example
 * {% page-header title: "titel" sub-title: "Uw producten in dit thema" %}{% endpage-header %}
 */
export const pageHeaderTag: LiquidTagDefinition = {
  component: LiquidPageHeader,
  props: { title: "title", "sub-title": "subTitle" },
  body: {},
};
