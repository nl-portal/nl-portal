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
import { gql } from "@apollo/client";

export const QUERY_GET_PERSOON_V2 = gql`
  query GetPersoonV2 {
    getPersoonV2 {
      burgerservicenummer
      geslacht {
        omschrijving
      }
      bewonersAantal
      geheimhoudingPersoonsgegevens
      naam {
        voornamen
        officialLastName
      }
      verblijfplaats {
        verblijfadres {
          officieleStraatnaam
          huisnummer
          huisletter
          huisnummertoevoeging
          postcode
          woonplaats
        }
        datumVan {
          datum
          langFormaat
          type
        }
      }
      geboorte {
        datum {
          datum
          langFormaat
          type
        }
        land {
          code
          omschrijving
        }
        plaats {
          code
          omschrijving
        }
      }
      nationaliteiten {
        nationaliteit {
          code
          omschrijving
        }
      }
    }
  }
`;
