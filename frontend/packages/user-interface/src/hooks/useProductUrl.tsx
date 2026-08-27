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
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { OpenProductProduct } from "@nl-portal/nl-portal-api";

const useProductUrl = (product: OpenProductProduct) => {
  const { paths, themes } = useOutletContext<RouterOutletContext>();

  if (!themes) return "#";

  const theme = Object.values(themes).find((theme) =>
    Object.values(theme.productTypes).some(
      (pt) => pt?.code === product.producttype?.code,
    ),
  );

  if (!theme || !themes[theme?.slug]) return "#";

  const productType = Object.values(
    themes[theme?.slug]?.productTypes || {},
  ).find((pt) => pt?.code === product.producttype?.code);

  if (!productType) return paths.themeOverview(theme?.slug);

  return paths.themeDetails(theme?.slug, productType?.slug, product.uuid);
};

export default useProductUrl;
