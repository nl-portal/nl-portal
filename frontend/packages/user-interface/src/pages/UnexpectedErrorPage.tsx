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
import { ReactNode, useEffect, useState } from "react";
import { FormattedMessage, IntlProvider, useIntl } from "react-intl";
import { Heading } from "@gemeente-denhaag/heading";
import { Header } from "@gemeente-denhaag/header";
import { Paragraph } from "@gemeente-denhaag/paragraph";
import { Link } from "@gemeente-denhaag/link";
import { StylesProvider } from "@gemeente-denhaag/stylesprovider";
import { Page, PageHeader } from "@gemeente-denhaag/page";
import { ResponsiveContent } from "@gemeente-denhaag/responsive-content";
import {
  DEFAULT_LOCALES,
  DEFAULT_MESSAGES,
  Messages,
} from "@nl-portal/nl-portal-localization";
import { ErrorBoundary } from "react-error-boundary";
import PageGrid from "../components/PageGrid";
import FullscreenSkeleton from "../components/FullscreenSkeleton";
import {
  loadFrontendConfig,
  applyFrontendTheme,
} from "../utils/frontend-config";

interface Props {
  customMessages?: Messages;
  headerLogo?: ReactNode;
  customHeader?: ReactNode;
}

const getLocale = () => {
  let locale = document.documentElement.lang;

  try {
    locale = localStorage.getItem("NL_PORTAL_LANG") || locale;
  } catch (err) {
    console.warn("Failed to read locale from localStorage:", err);
  }

  return locale.toLowerCase().startsWith("en")
    ? DEFAULT_LOCALES.ENGLISH
    : DEFAULT_LOCALES.DUTCH;
};

const ErrorPageContent = ({
  headerLogo,
  customHeader,
}: Omit<Props, "customMessages">) => {
  const intl = useIntl();
  const [logoUrl, setLogoUrl] = useState<string>();
  const [isLoadingTheme, setIsLoadingTheme] = useState(
    Boolean(window.REST_URI),
  );
  const title = intl.formatMessage({
    id: "unexpectedError.title",
  });

  useEffect(() => {
    document.title = title;
  }, [title]);

  useEffect(() => {
    if (!window.REST_URI) return;

    const loadTheme = async () => {
      try {
        const features = await loadFrontendConfig(window.REST_URI);

        applyFrontendTheme(features);
        setLogoUrl(features.theme?.logo);
      } catch (err) {
        console.warn("Failed to load error page theme:", err);
      } finally {
        setIsLoadingTheme(false);
      }
    };

    void loadTheme();
  }, []);

  if (isLoadingTheme) return <FullscreenSkeleton />;

  const logo = {
    href: "/",
    children: logoUrl ? <img src={logoUrl} alt="" /> : headerLogo,
    "aria-label": "NL Portal",
  };

  const defaultHeader = <Header logo={logo} />;

  return (
    <Page>
      <PageHeader>
        {customHeader ? (
          <ErrorBoundary fallback={defaultHeader}>{customHeader}</ErrorBoundary>
        ) : (
          defaultHeader
        )}
      </PageHeader>
      <ResponsiveContent className="denhaag-page-content">
        <main
          id="main-content"
          className="denhaag-page-content__main"
          aria-labelledby="unexpected-error-title"
        >
          <PageGrid variant="small">
            <Heading level={1} id="unexpected-error-title">
              {title}
            </Heading>
            <FormattedMessage
              id="unexpectedError.description"
              values={{
                p: (chunks) => <Paragraph>{chunks}</Paragraph>,
                link: (chunks) => <Link href="/">{chunks}</Link>,
              }}
            />
          </PageGrid>
        </main>
      </ResponsiveContent>
    </Page>
  );
};

const UnexpectedErrorPage = ({ customMessages, ...pageProps }: Props) => {
  const [locale] = useState(getLocale);

  return (
    <IntlProvider
      locale={locale}
      defaultLocale={DEFAULT_LOCALES.DUTCH}
      messages={{ ...DEFAULT_MESSAGES[locale], ...customMessages?.[locale] }}
    >
      <StylesProvider lang={locale.split("-")[0]}>
        <ErrorPageContent {...pageProps} />
      </StylesProvider>
    </IntlProvider>
  );
};

export default UnexpectedErrorPage;
