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
export const config = {
  OIDC_URL: window.OIDC_URL,
  OIDC_REALM: window.OIDC_REALM,
  OIDC_CLIENT_ID: window.OIDC_CLIENT_ID,
  OIDC_REDIRECT_URI: window.OIDC_REDIRECT_URI,
  OIDC_POST_LOGOUT_REDIRECT_URI: window.OIDC_POST_LOGOUT_REDIRECT_URI,
  OIDC_AUTO_IDLE_SESSION_LOGOUT: window.OIDC_AUTO_IDLE_SESSION_LOGOUT,
  OIDC_IDLE_TIMEOUT_MINUTES: window.OIDC_IDLE_TIMEOUT_MINUTES,
  GRAPHQL_URI: window.GRAPHQL_URI,
  REST_URI: window.REST_URI,
};
