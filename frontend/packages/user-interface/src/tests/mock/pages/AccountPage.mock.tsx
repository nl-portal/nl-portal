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
import AccountPage from "../../../pages/AccountPage";
import TestProvider, { testPaths } from "../../../providers/TestProvider";
import { getPersoon } from "../data/persoon.mock";
import { getProduct } from "../data/product.mock";
import { getUnopenedBerichten } from "../data/unopened-berichten";

const routes = [
  {
    path: testPaths.account,
    children: [
      {
        index: true,
        element: <AccountPage />,
      },
    ],
  },
];

export const MockAccountPage = () => (
  <TestProvider
    mocks={[getPersoon, getProduct, getUnopenedBerichten]}
    routes={routes}
    initialIndex={0}
    initialEntries={[testPaths.account]}
  ></TestProvider>
);
