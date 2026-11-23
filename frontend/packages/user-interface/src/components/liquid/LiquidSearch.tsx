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
import { useContext, useId, type FormEvent } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { FormField } from "@gemeente-denhaag/form-field";
import { FormLabel } from "@gemeente-denhaag/form-label";
import { TextInput } from "@gemeente-denhaag/text-input";
import { Button } from "@gemeente-denhaag/button";
import { SearchIcon } from "@gemeente-denhaag/icons";
import PortalPageBindingContext from "../../contexts/PortalPageBindingContext";

type Props = {
  binding?: string;
  param?: string;
  title?: string;
  pageParam?: string;
};

/**
 * A search field that refetches [binding] with [param] set to the submitted text, resetting [pageParam].
 * It starts with the binding's current value. Without a PortalPageBindingContext it is disabled.
 */
const LiquidSearch = ({ binding, param, title, pageParam }: Props) => {
  const intl = useIntl();
  const id = useId();
  const context = useContext(PortalPageBindingContext);
  const interactive = Boolean(context && binding && param);
  const value =
    (binding && param && context?.meta(binding)?.params[param]) || "";

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!context || !binding || !param) return;
    const text = new FormData(event.currentTarget).get("search");
    context.refetch(binding, {
      [param]: typeof text === "string" ? text.trim() : "",
      ...(pageParam ? { [pageParam]: "" } : {}),
    });
  };

  return (
    <form role="search" onSubmit={onSubmit}>
      <FormField>
        {title && (
          <FormLabel htmlFor={id}>
            {intl.formatMessage({ id: title, defaultMessage: title })}
          </FormLabel>
        )}
        <TextInput
          id={id}
          name="search"
          key={value}
          defaultValue={value}
          disabled={!interactive}
          iconEnd={<SearchIcon />}
        />
        <Button type="submit" disabled={!interactive}>
          <FormattedMessage id="portalPage.search" />
        </Button>
      </FormField>
    </form>
  );
};

export default LiquidSearch;
