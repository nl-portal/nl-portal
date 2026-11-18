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
import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  createIntl,
  RawIntlProvider,
  useIntl,
  type IntlShape,
} from "react-intl";
import { renderTemplate } from "../utils/liquid/renderTemplate";

export interface LiquidParserProps {
  template: string;
  data: Record<string, unknown>;
  onError?: (error: Error) => void;
  /** Messages of the template itself, layered over the app messages for the `i18n` filter and `title` arguments. */
  messages?: Record<string, string>;
}

const LiquidParser = ({
  template,
  data,
  onError,
  messages,
}: LiquidParserProps) => {
  const appIntl = useIntl();
  const intl = useMemo(
    () =>
      messages && Object.keys(messages).length
        ? createIntl({
            locale: appIntl.locale,
            defaultLocale: appIntl.defaultLocale,
            formats: appIntl.formats,
            onError: appIntl.onError,
            messages: {
              ...(appIntl.messages as Record<string, string>),
              ...messages,
            },
          })
        : appIntl,
    [appIntl, messages],
  );
  const [result, setResult] = useState<{
    template: string;
    data: Record<string, unknown>;
    content: ReactNode;
    intl: IntlShape;
  }>();

  useEffect(() => {
    let active = true;
    renderTemplate(template, data, intl)
      .then((content) => {
        if (active) setResult({ template, data, intl, content });
      })
      .catch((error: unknown) => {
        if (!active) return;
        setResult(undefined);
        onError?.(error instanceof Error ? error : new Error(String(error)));
      });
    return () => {
      active = false;
    };
  }, [template, data, intl, onError]);

  if (
    result?.template !== template ||
    result.data !== data ||
    result.intl !== intl
  ) {
    return null;
  }
  return intl === appIntl ? (
    result.content
  ) : (
    <RawIntlProvider value={intl}>{result.content}</RawIntlProvider>
  );
};

export default LiquidParser;
