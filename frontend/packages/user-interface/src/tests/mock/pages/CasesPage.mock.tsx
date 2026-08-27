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
import CasesPage from "../../../pages/CasesPage";
import TestProvider, { testPaths } from "../../../providers/TestProvider";
import { getProduct } from "../data/product.mock";
import { getUnopenedBerichten } from "../data/unopened-berichten";
import { getZaken } from "../data/zaken.mock";

const routes = [
  {
    path: testPaths.cases,
    children: [
      {
        index: true,
        element: <CasesPage />,
      },
    ],
  },
];

export const MockCasesPage = () => (
  <TestProvider
    mocks={[
      getProduct,
      getUnopenedBerichten,
      getZaken({ pageSize: 10, isOpen: true }),
      getZaken({ pageSize: 10, isOpen: false }),
      getZaken({ pageSize: 10, isOpen: false, page: 1 }),
      getZaken({ pageSize: 10, isOpen: false, page: 1, identificatie: "" }),
    ]}
    routes={routes}
    initialIndex={0}
    initialEntries={[testPaths.cases]}
  ></TestProvider>
);
