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
import { useContext } from "react";
import { Tab, TabList, TabsContainer, TabText } from "@gemeente-denhaag/tab";
import PageBindingContext from "../../contexts/PageBindingContext";
import type { LiquidFilterOption } from "./LiquidFilter";

type Props = {
  binding?: string;
  param?: string;
  pageParam?: string;
  options: LiquidFilterOption[];
};

/**
 * Tabs that refetch [binding] with [param] set to the chosen value, resetting [pageParam]. The selected tab
 * follows the binding's current params. Without a PageBindingContext the tabs don't react.
 */
const LiquidTabs = ({ binding, param, pageParam, options }: Props) => {
  const context = useContext(PageBindingContext);
  const interactive = Boolean(context && binding && param);
  const value =
    (binding && param && context?.meta(binding)?.params[param]) || "";

  const select = (next: string) => {
    if (!context || !binding || !param || next === value) return;
    context.refetch(binding, {
      [param]: next,
      ...(pageParam ? { [pageParam]: "" } : {}),
    });
  };

  return (
    <TabsContainer>
      <TabList>
        {options.map((option) => (
          <Tab
            key={option.value}
            selected={option.value === value}
            tabIndex={interactive ? 0 : -1}
            onClick={() => select(option.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                select(option.value);
              }
            }}
          >
            <TabText>{option.label}</TabText>
          </Tab>
        ))}
      </TabList>
    </TabsContainer>
  );
};

export default LiquidTabs;
