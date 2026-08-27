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
import { BrpNaam, BrpNationaliteit } from "@nl-portal/nl-portal-api";

const getNationalitiesString = (
  nationalities?: BrpNationaliteit[] | null,
): string => {
  if (Array.isArray(nationalities)) {
    return nationalities
      .map((nationality) => nationality?.nationaliteit?.omschrijving)
      .filter((nationalityString) => nationalityString)
      .reduce((accumulatedString, currentNationalityString) => {
        if (accumulatedString === "") {
          return currentNationalityString;
        }
        return `${accumulatedString}, ${currentNationalityString}`;
      }, "") as string;
  }

  return "";
};

const getStreetString = (
  street?: string | null,
  number?: string | null,
  letter?: string | null,
  addition?: string | null,
): string => {
  const houseNr = number ? `${number}${letter ?? ""}` : null;
  return [street, houseNr, addition].filter(Boolean).join(" ");
};

const getPostalCodeCityString = (
  postalCode: string | null | undefined,
  city: string | null | undefined,
): string => {
  if (city) {
    if (postalCode) {
      return `${postalCode} ${city}`;
    }

    return city;
  }

  return "";
};

const capitalizeFirstLetter = (text: string): string =>
  text.charAt(0).toUpperCase() + text.slice(1);

const getFullName = (
  name?: Pick<BrpNaam, "voornamen" | "officialLastName">,
) => {
  const firstNames = name?.voornamen;
  const officialLastName = name?.officialLastName;
  const fullName = `${firstNames} ${officialLastName}`;

  if (firstNames && officialLastName) return fullName;
  if (firstNames) return firstNames;
  if (officialLastName) return officialLastName;
  return "";
};

export {
  getNationalitiesString,
  getStreetString,
  getPostalCodeCityString,
  capitalizeFirstLetter,
  getFullName,
};
