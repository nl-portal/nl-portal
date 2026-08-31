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
import { IntlProvider } from "react-intl";
import { DEFAULT_LOCALES } from "../i18n/default-locales";
import { DEFAULT_MESSAGES } from "../i18n/messages/messages";
import LocaleContext from "../contexts/LocaleContext";
import { useState } from "react";

interface Props {
  children: React.ReactNode;
}

const MockWrapper = ({ children }: Props) => {
  const [currentLocale, setCurrentLocale] = useState(DEFAULT_LOCALES.ENGLISH);
  const [supportedLocales] = useState(Object.values([DEFAULT_LOCALES.ENGLISH]));
  const hrefLang = "en";
  const messages = DEFAULT_MESSAGES[currentLocale];

  return (
    <LocaleContext.Provider
      value={{ currentLocale, supportedLocales, setCurrentLocale, hrefLang }}
    >
      <IntlProvider
        locale={currentLocale}
        messages={messages}
        onError={() => {}}
      >
        {children}
      </IntlProvider>
    </LocaleContext.Provider>
  );
};

export default MockWrapper;
