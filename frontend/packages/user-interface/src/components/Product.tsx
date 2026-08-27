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
import type { OpenProductProduct } from "@nl-portal/nl-portal-api";
import PortalLink from "./PortalLink";
import { ActionSingle } from "@gemeente-denhaag/action";
import useProductUrl from "../hooks/useProductUrl";

interface Props {
  product: OpenProductProduct;
}

const Product = ({ product }: Props) => {
  const productLink = useProductUrl(product) ?? "";

  return (
    <ActionSingle link={productLink} Link={PortalLink}>
      {product.naam}
    </ActionSingle>
  );
};

export default Product;
