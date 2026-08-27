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
import React, { useEffect } from "react";
import { useState } from "react";
import { IntlProvider } from "react-intl";
import deepmerge from "deepmerge";
import { DEFAULT_LOCALES } from "../i18n/default-locales";
import LocaleContext from "../contexts/LocaleContext";
import { Locales } from "../interfaces/locales";
import { Messages } from "../interfaces/messages";
import { DEFAULT_MESSAGES } from "../i18n/messages/messages";

interface Props {
  children: React.ReactNode;
  customMessages?: Messages;
  customLocales?: Locales;
}

const LocalizationProvider = ({
  children,
  customMessages,
  customLocales,
}: Props) => {
  const LOCAL_STORAGE_LANG_KEY = "NL_PORTAL_LANG";
  const savedLocale = localStorage.getItem(LOCAL_STORAGE_LANG_KEY);

  const messages = customMessages
    ? deepmerge(DEFAULT_MESSAGES, customMessages)
    : DEFAULT_MESSAGES;
  const locales = customLocales || DEFAULT_LOCALES;

  const savedLocaleIndex = Object.values(locales).findIndex(
    (locale) => locale === savedLocale,
  );
  const localeToUse =
    locales[
      Object.keys(locales)[savedLocaleIndex !== -1 ? savedLocaleIndex : 0]
    ];

  const [currentLocale, setCurrentLocale] = useState(localeToUse);
  const [supportedLocales] = useState(Object.values(locales));

  const hrefLang = currentLocale.toLocaleLowerCase().split("-")[0];

  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_LANG_KEY, currentLocale);
    document.documentElement.lang = hrefLang;
  }, [currentLocale, hrefLang]);

  return (
    <LocaleContext.Provider
      value={{ currentLocale, supportedLocales, setCurrentLocale, hrefLang }}
    >
      <IntlProvider
        locale={currentLocale}
        messages={messages[currentLocale]}
        onError={(error) => {
          if (error.code === "MISSING_TRANSLATION") {
            console.log("Missing translation", error.message);
            return;
          }
          throw error;
        }}
      >
        {children}
      </IntlProvider>
    </LocaleContext.Provider>
  );
};

export default LocalizationProvider;
