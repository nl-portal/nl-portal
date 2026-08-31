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
import { Messages, DEFAULT_LOCALES } from "@nl-portal/nl-portal-localization";

export const EN_GB_MESSAGES: Messages = {
  [DEFAULT_LOCALES.ENGLISH]: {
    "app.appName": "NL Portal App",
    "case.bezwaar-behandelen.title": "Notice of objection",
    "case.bezwaar-behandelen.status.intake-afgerond": "Intake completed",
    "case.bezwaar-behandelen.status.indieningsvereisten-getoetst":
      "Submission requirements reviewed",
    "case.bezwaar-behandelen.status.bezwaar-beoordeeld": "Objection reviewed",
    "case.bezwaar-behandelen.status.hoorzitting-gehouden": "Hearing held",
    "case.bezwaar-behandelen.status.concept-besluit-opgesteld":
      "Decision drafted",
    "case.bezwaar-behandelen.status.besluit-vastgesteld": "Decision adopted",
    "case.bezwaar-behandelen.status.zaak-afgerond": "Case closed",
    "footerColumns.theHague": "The Hague",
    "footerColumns.disclaimers": "Disclaimers",
    "footerLinks.goToTheHague": "Go to denhaag.nl",
    "footerLinks.accessibility": "Accessibility declaration",
    "footerLinks.dataProtection": "Data Protection Declaration",
    "footerLinks.proclaimer": "Proclaimer",
    "overview.alertTitle": "Maintenance",
    "overview.alertText":
      "The application is currently undergoing maintenance. Functionality may be limited or not work as intended. Please check back later.",
    "auth.inactive.text":
      "You have been idle for too long. You will soon be automatically logged out of the secure environment. If you have entered any data, it will be lost. Click 'Stay logged in' to continue or 'Log out' to stop.",
  },
};
