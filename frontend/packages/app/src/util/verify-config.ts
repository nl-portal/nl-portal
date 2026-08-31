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
import { config } from "../constants/config";

const verifyConfig = () => {
  if (!config.OIDC_CLIENT_ID) {
    console.log("OIDC_CLIENT_ID is not set");
    return null;
  }

  if (!config.OIDC_REALM) {
    console.log("OIDC_REALM is not set");
    return null;
  }

  if (!config.OIDC_URL) {
    console.log("OIDC_URL is not set");
    return null;
  }

  if (!config.OIDC_REDIRECT_URI) {
    console.log("OIDC_REDIRECT_URI is not set");
    return null;
  }

  if (!config.GRAPHQL_URI) {
    console.log("GRAPHQL_URI is not set");
    return null;
  }

  if (!config.REST_URI) {
    console.log("REST_URI is not set");
    return null;
  }

  return true;
};

export default verifyConfig;
