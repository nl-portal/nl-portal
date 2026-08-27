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
import TestProvider, {
  testPaths as paths,
} from "../../../providers/TestProvider";
import { OverviewPage } from "../../..";
import { getZaken } from "../data/zaken.mock";
import { getTaken } from "../data/taken.mock";

const route = [
  {
    path: paths.overview,
    children: [
      {
        index: true,
        element: <OverviewPage />,
      },
    ],
  },
];

const routeShowLessTasks = [
  {
    path: paths.overview,
    children: [
      {
        index: true,
        element: <OverviewPage fetchTasksLength={2} />,
      },
    ],
  },
];

export const MockOverviewPage = () => (
  <TestProvider
    mocks={[getZaken({ pageSize: 4 }), getTaken({ pageSize: 5 })]}
    routes={route}
  ></TestProvider>
);

export const MockOverviewPageLessTasks = () => (
  <TestProvider
    mocks={[getZaken({ pageSize: 4 }), getTaken({ pageSize: 2 })]}
    routes={routeShowLessTasks}
  ></TestProvider>
);

export const MockOverviewPagePagination = () => (
  <TestProvider
    mocks={[
      getZaken({ pageSize: 4, totalElements: 20 }),
      getTaken({ pageSize: 5 }),
    ]}
    routes={route}
  ></TestProvider>
);
