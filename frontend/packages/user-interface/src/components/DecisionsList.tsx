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
import { ActionSingle } from "@gemeente-denhaag/action";
import { useActionLabels } from "@nl-portal/nl-portal-localization";
import { useIntl } from "react-intl";
import SectionHeader from "./SectionHeader";
import Skeleton from "react-loading-skeleton";
import PortalLink from "./PortalLink";

interface Props {
  loading: boolean;
  titleTranslationId?: string | null;
  decisions?: {
    type: {
      value: string;
    };
    action: {
      value: string;
    };
    text: {
      value: string;
    };
  }[];
}

const DecisionsList = ({
  loading,
  titleTranslationId = "decisionsList.title",
  decisions,
}: Props) => {
  const intl = useIntl();
  const labels = useActionLabels();
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;

  if (loading) {
    return (
      <section>
        <SectionHeader title={title} />
        <Skeleton height={60} />
        <Skeleton height={60} />
        <Skeleton height={60} />
      </section>
    );
  }

  if (!decisions || decisions.length === 0) {
    return null;
  }

  return (
    <section>
      <SectionHeader title={title} />
      {decisions.map((decision) => {
        if (decision.type.value === "LINK" || decision.type.value === "INTERN")
          return (
            <ActionSingle
              key={decision.action.value}
              labels={labels}
              link={decision.action.value}
              Link={PortalLink}
            >
              {decision.text.value}
            </ActionSingle>
          );
      })}
    </section>
  );
};

export default DecisionsList;
