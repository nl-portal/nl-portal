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
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import {
  ApolloClient,
  ApolloLink,
  HttpLink,
  InMemoryCache,
  InMemoryCacheConfig,
} from "@apollo/client";
import { ApolloProvider } from "@apollo/client/react";
import {
  formatUrlTrailingSlash,
  OidcContext,
} from "@nl-portal/nl-portal-authentication";
import React from "react";
import { defaultInMemoryCacheOptions } from "../constants/apollo-cache";

interface ContextProps {
  restUri: string;
}

const ApiContext = createContext({} as ContextProps);

interface Props {
  children: React.ReactNode;
  graphqlUri: string;
  restUri: string;
  inMemoryCacheOptions?: InMemoryCacheConfig;
}

export const ApiProvider = ({
  children,
  graphqlUri,
  restUri,
  inMemoryCacheOptions = defaultInMemoryCacheOptions,
}: Props) => {
  const formattedGraphqlUri = formatUrlTrailingSlash(graphqlUri, false);
  const formattedRestUri = formatUrlTrailingSlash(restUri, false);
  const { oidcToken } = useContext(OidcContext);

  const getLink = useCallback(
    (oidcToken: string) =>
      new ApolloLink((operation, forward) => {
        operation.setContext({
          headers: {
            authorization: `Bearer ${oidcToken}`,
          },
        });
        return forward(operation);
      }).concat(new HttpLink({ uri: formattedGraphqlUri })),
    [formattedGraphqlUri],
  );

  const [client] = useState(
    () =>
      new ApolloClient({
        cache: new InMemoryCache(inMemoryCacheOptions),
        link: getLink(oidcToken),
      }),
  );

  useEffect(() => {
    client.setLink(getLink(oidcToken));
  }, [oidcToken, client, getLink]);

  sessionStorage.setItem("REST_URI", formattedRestUri);

  if (!oidcToken) return null;

  return (
    <ApiContext.Provider value={{ restUri: formattedRestUri }}>
      <ApolloProvider client={client}>{children}</ApolloProvider>
    </ApiContext.Provider>
  );
};

export default ApiContext;
