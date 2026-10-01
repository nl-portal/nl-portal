import { gql } from "@apollo/client";

export const QUERY_GET_AFSPRAKEN = gql`
  query GetAfspraken(
    $identificaties: [IdentificatieInput!]!
    $van: String
    $tot: String
  ) {
    getAfspraken(identificaties: $identificaties, van: $van, tot: $tot) {
      afspraken {
        afspraakIdentificatie
        onderwerp
        geplandAanvangsmoment
        geplandEindmoment
      }
      totalCount
    }
  }
`;
