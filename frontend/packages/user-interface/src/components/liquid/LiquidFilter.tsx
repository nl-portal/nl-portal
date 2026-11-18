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
import { useContext, useId } from "react";
import { useIntl } from "react-intl";
import { Select, SelectOption } from "@gemeente-denhaag/select";
import PortalPageBindingContext from "../../contexts/PortalPageBindingContext";

export type LiquidFilterOption = { value: string; label: string };

type Props = {
  binding?: string;
  param?: string;
  title?: string;
  pageParam?: string;
  options: LiquidFilterOption[];
};

/**
 * A select that refetches [binding] with [param] set to the chosen value; the empty value removes
 * the override. [pageParam] is reset at the same time. Without a PortalPageBindingContext it is disabled.
 */
const LiquidFilter = ({ binding, param, title, pageParam, options }: Props) => {
  const intl = useIntl();
  const id = useId();
  const context = useContext(PortalPageBindingContext);
  const interactive = Boolean(context && binding && param);
  const value =
    (binding && param && context?.meta(binding)?.params[param]) || "";

  const onChange = (next: string) => {
    if (!context || !binding || !param) return;
    context.refetch(binding, {
      [param]: next,
      ...(pageParam ? { [pageParam]: "" } : {}),
    });
  };

  return (
    <div>
      {title && (
        <label htmlFor={id}>
          {intl.formatMessage({ id: title, defaultMessage: title })}
        </label>
      )}
      <Select
        id={id}
        value={value}
        disabled={!interactive}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <SelectOption key={option.value} value={option.value}>
            {option.label}
          </SelectOption>
        ))}
      </Select>
    </div>
  );
};

export default LiquidFilter;
