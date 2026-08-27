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
window.OIDC_URL = "http://localhost:8082/auth";
window.OIDC_REALM = "nlportal";
window.OIDC_CLIENT_ID = "nl-portal";
window.OIDC_REDIRECT_URI = "http://localhost:3000/keycloak/callback";
window.OIDC_POST_LOGOUT_REDIRECT_URI = "http://localhost:3000";
window.OIDC_AUTO_IDLE_SESSION_LOGOUT = "true";
window.OIDC_IDLE_TIMEOUT_MINUTES = "15";
window.GRAPHQL_URI = "http://localhost:8080/graphql";
window.REST_URI = "http://localhost:8080/api";
