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
import { useContext, useEffect } from "react";
import { FormattedDate, FormattedMessage, FormattedNumber } from "react-intl";
import { themes } from "../constants/themes";
import {
  NotificationContext,
  ThemeOverviewPage,
} from "@nl-portal/nl-portal-user-interface";
import { capitalizeFirstLetter, currencyFormat } from "../util/product-format";

const ParkerenOverview = () => {
  const { dispatch } = useContext(NotificationContext);

  useEffect(() => {
    dispatch({
      type: "CREATE",
      id: "parkingOverview",
      notification: {
        variant: "info",
        title: <FormattedMessage id="theme.sample.infoTitle" />,
        text: <FormattedMessage id="theme.sample.infoTitle" />,
      },
    });
  }, [dispatch]);

  return (
    <ThemeOverviewPage
      slug={themes.parkeren.slug}
      productenSettings={[
        {
          productTypeSlug: themes.parkeren.productTypes.vergunningen?.slug,
          productTypeCodes: [themes.parkeren.productTypes.vergunningen?.code],
          titleTranslationId: "Vergunningen",
          headerTranslationIds: [
            "Naam",
            "Startdatum",
            "Einddatum",
            "Status",
            "Prijs",
            "Test",
          ],
          dataMapping: [
            "naam",
            (product) => (
              <FormattedDate value={product?.startDatum ?? undefined} />
            ),
            (product) => (
              <FormattedDate value={product?.eindDatum ?? undefined} />
            ),
            (product) =>
              capitalizeFirstLetter(product?.status.toLowerCase() ?? ""),
            (product) => {
              return (
                <FormattedNumber
                  value={product?.prijs ?? 0}
                  {...currencyFormat}
                />
              );
            },
            "dataobject.test",
          ],
        },
        {
          productTypeSlug:
            themes.parkeren.productTypes.bezoekersvergunningen?.slug,
          productTypeCodes: [
            themes.parkeren.productTypes.bezoekersvergunningen?.code,
          ],
          titleTranslationId: "Bezoekersvergunningen",
          headerTranslationIds: ["Naam", "Startdatum", "Status", "Prijs"],
          dataMapping: [
            "naam",
            "startDatum",
            (product) =>
              capitalizeFirstLetter(product?.status.toLowerCase() ?? ""),
            (product) => {
              return (
                <FormattedNumber
                  value={product?.prijs ?? 0}
                  {...currencyFormat}
                />
              );
            },
          ],
        },
      ]}
    />
  );
};

export default ParkerenOverview;
