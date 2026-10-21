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
export interface Features {
  theme: {
    logo: string;
    style: string;
  };
  properties: {
    custom: Record<string, string | number>;
    messageCountPollingInterval: number;
    myAddressChangeUrl: string;
    myAddressResearchMoreInfoUrl: string;
    myAddressResearchUrl: string;
    myBrpChangeUrl: string;
    myBrpConfidentiallyChangeUrl: string;
    myGenderChangeUrl: string;
    myNameChangeUrl: string;
    overviewCurrentCasesPreviewLength: number;
    overviewMaintenanceAlertTextEn: string;
    overviewMaintenanceAlertTextNl: string;
    overviewMaintenanceAlertTitleEn: string;
    overviewMaintenanceAlertTitleNl: string;
    themeClass: string;
  };
  toggles: {
    custom: Record<string, boolean>;
    casesContactMomentsEnabled: boolean;
    casesPartialSearchEnabled: boolean;
    casesResultExplanationEnabled: boolean;
    messageCountEnabled: boolean;
    myInhabitantCountEnabled: boolean;
    openProductEnabled: boolean;
    overviewIntroEnabled: boolean;
    overviewMaintenanceAlertEnabled: boolean;
    menuEntriesEnabled?: boolean;
  };
}

type FeaturesResponse = Omit<Features, "properties" | "toggles"> & {
  properties: Omit<Features["properties"], "custom"> & { custom: string };
  toggles: Omit<Features["toggles"], "custom"> & { custom: string };
};

export const loadFrontendConfig = async (
  restUri: string,
): Promise<Features> => {
  const response = await fetch(`${restUri}/public/frontend`);

  if (!response.ok) {
    throw new Error(`Frontend config request failed: ${response.status}`);
  }

  const json = (await response.json()) as FeaturesResponse;

  return {
    ...json,
    properties: {
      ...json.properties,
      custom: JSON.parse(json.properties.custom || "{}"),
    },
    toggles: {
      ...json.toggles,
      custom: JSON.parse(json.toggles.custom || "{}"),
    },
  };
};

export const applyFrontendTheme = (features: Features) => {
  const styleId = "nl-portal-frontend-theme";
  const styleNode =
    document.querySelector<HTMLStyleElement>(`style#${styleId}`) ||
    document.createElement("style");
  const themeClass = features.properties.themeClass;

  styleNode.id = styleId;
  styleNode.nonce =
    document.querySelector<HTMLMetaElement>("meta[name='csp-nonce']")
      ?.content || "";
  styleNode.textContent = features.theme.style ?? "";
  document.head.appendChild(styleNode);

  if (themeClass) {
    document.documentElement.classList.add(themeClass);
  }
};
