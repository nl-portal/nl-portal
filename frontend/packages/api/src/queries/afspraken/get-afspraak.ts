import { gql } from "@apollo/client";

export const QUERY_GET_AFSPRAAK = gql`
  query GetAfspraak($id: ID!) {
    getAfspraak(id: $id) {
      afspraakIdentificatie
      afspraakSoort
      onderwerp
      geplandAanvangsmoment
      geplandEindmoment
      aanmeldkenmerkAlfanumeriek
      onlineAdres
      afspraakLocatie {
        naam
        bezoekadres {
          adresregel1
          adresregel2
        }
      }
      activiteiten {
        naamActiviteitsoort
        aantal
        aanduidingWatMeeTeNemen
      }
      contactInformatie {
        naam
        emailadres
        telefoonnummer
        adres {
          adresregel1
          adresregel2
        }
      }
    }
  }
`;
