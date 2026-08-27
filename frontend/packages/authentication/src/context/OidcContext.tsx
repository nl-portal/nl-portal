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
import React, { useMemo } from "react";
import { AuthProvider } from "react-oidc-context";
import { User } from "oidc-client-ts";
import { useState } from "react";
import { DecodedToken } from "../interfaces/decoded-token";
import ProtectedApp from "../components/ProtectedApp";
import { decodeToken } from "../utils/decode-token";
import generateRedirectUri from "../utils/generate-redirect-uri";
import { filterEmptyParams } from "../utils/filter-empty-params.ts";

export type AuthenticationMethods = {
  person?: string[];
  company?: string[];
  proxy?: string[];
};

export type OidcConfig = {
  url: string;
  clientId: string;
  realm: string;
  redirectUri: string;
  postLogoutRedirectUri?: string;
};

export type SessionLengthManagementProps = {
  autoIdleSessionLogout?: boolean;
  idleTimeoutMinutes?: number;
};

export type OidcProviderProps = OidcConfig &
  SessionLengthManagementProps & {
    children: React.ReactNode;
    authenticationMethods?: AuthenticationMethods;
    extraQueryParams?: Record<string, string>;
  };

export interface OidcContextInterface {
  oidcToken: string;
  setOidcToken: (token: string) => void;
  decodedToken: DecodedToken | undefined;
  authenticationMethods?: AuthenticationMethods;
}

const OidcContext = React.createContext<OidcContextInterface>(
  {} as OidcContextInterface,
);

export const OidcProvider = ({
  authenticationMethods,
  url,
  clientId,
  realm,
  redirectUri,
  postLogoutRedirectUri,
  children,
  autoIdleSessionLogout,
  idleTimeoutMinutes,
  extraQueryParams,
}: OidcProviderProps) => {
  const [oidcToken, setOidcToken] = useState("");
  const decodedToken = useMemo(
    () => (oidcToken ? decodeToken(oidcToken) : undefined),
    [oidcToken],
  );

  const oidcConfig = {
    authority: `${url}/realms/${realm}`,
    client_id: clientId,
    redirect_uri: generateRedirectUri(redirectUri, true),
    accessTokenExpiringNotificationTime: 15,
    post_logout_redirect_uri:
      postLogoutRedirectUri || generateRedirectUri(redirectUri, false),
    extraQueryParams: filterEmptyParams(extraQueryParams),
  };

  const onSigninCallback = (user: User | undefined) => {
    // clear authentication params once logged in
    window.history.replaceState({}, document.title, window.location.pathname);

    if (!user?.access_token) return;
    setOidcToken(user?.access_token);
  };

  return (
    <OidcContext.Provider
      value={{
        oidcToken,
        setOidcToken,
        decodedToken,
        authenticationMethods,
      }}
    >
      <AuthProvider {...oidcConfig} onSigninCallback={onSigninCallback}>
        <ProtectedApp
          autoIdleSessionLogout={autoIdleSessionLogout}
          idleTimeoutMinutes={idleTimeoutMinutes}
        >
          {children}
        </ProtectedApp>
      </AuthProvider>
    </OidcContext.Provider>
  );
};

export default OidcContext;
