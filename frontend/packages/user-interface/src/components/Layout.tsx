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
import { AnchorHTMLAttributes, ReactNode } from "react";
import { StylesProvider } from "@gemeente-denhaag/stylesprovider";
import {
  Page as PageWrapper,
  PageHeader,
  PageFooter,
} from "@gemeente-denhaag/page";
import { ResponsiveContent } from "@gemeente-denhaag/responsive-content";
import Header from "./Header";
import Menu from "./Menu";
import { Footer, FooterProps } from "@gemeente-denhaag/footer";
import { HelmetProvider } from "react-helmet-async";
import { Outlet } from "react-router";
import PageMetaData from "./PageMetaData";
import { Paths } from "../interfaces/paths";
import "@utrecht/document-css";
import { SkipLink } from "@gemeente-denhaag/skip-link";
import styles from "./Layout.module.scss";
import { FormattedMessage } from "react-intl";
import { Themes } from "../interfaces/themes";

interface LayoutComponentProps {
  paths: Paths;
  themes?: Themes;
  headerLogo?: AnchorHTMLAttributes<HTMLAnchorElement>;
  customHeader?: ReactNode;
  customFooter?: ReactNode;
  footerData?: FooterProps;
}

const Layout = ({
  paths,
  themes,
  headerLogo,
  customHeader,
  customFooter,
  footerData,
}: LayoutComponentProps) => {
  return (
    <StylesProvider className={styles.layout}>
      <SkipLink className={styles.skiplink} href="#main-content">
        <FormattedMessage id="layout.skip" />
      </SkipLink>
      <HelmetProvider>
        <PageWrapper>
          <PageHeader>
            {customHeader || <Header logo={headerLogo} paths={paths} />}
          </PageHeader>
          <ResponsiveContent className="denhaag-page-content denhaag-responsive-content--sidebar">
            <Menu />
            <main id="main-content" className="denhaag-page-content__main">
              <PageMetaData />
              {<Outlet context={{ paths, themes }} />}
            </main>
          </ResponsiveContent>
          {(footerData || customFooter) && (
            <PageFooter>
              {footerData ? <Footer {...footerData} /> : customFooter}
            </PageFooter>
          )}
        </PageWrapper>
      </HelmetProvider>
    </StylesProvider>
  );
};

export default Layout;
