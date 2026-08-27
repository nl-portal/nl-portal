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
import { TaakSoort, TaakV2 } from "@nl-portal/nl-portal-api";
import { TaakKoppelingRegistratie } from "../interfaces/taak-koppeling-registratie";

const useTaskUrl = (task: TaakV2, openInContext?: boolean) => {
  const { paths } = useOutletContext<RouterOutletContext>();
  if (openInContext && task.koppeling.value) {
    if (
      task.koppeling.registratie.toUpperCase() === TaakKoppelingRegistratie.Zaak
    )
      return paths.case(task.koppeling.value);
  }
  if (task.soort === TaakSoort.Url) return task.url?.uri;
  return paths.task(task.id);
};

export default useTaskUrl;
