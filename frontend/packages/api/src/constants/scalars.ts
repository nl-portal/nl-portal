export const scalarConfig = {
  strictScalars: true,
  scalars: {
    ID: {
      input: "string",
      output: "string",
    },
    UUID: {
      input: "string",
      output: "string",
    },
    PositiveFloat: {
      input: "number",
      output: "number",
    },
    Date: {
      input: "string",
      output: "string",
    },
    DateTime: {
      input: "string",
      output: "string",
    },
    LocalDateTime: {
      input: "string",
      output: "string",
    },
    LocalTime: {
      input: "string",
      output: "string",
    },
    ZonedDateTime: {
      input: "string",
      output: "string",
    },
    Locale: {
      input: "string",
      output: "string",
    },
    JSON: {
      input: "any",
      output: "any",
    },
    BigDecimal: {
      input: "number",
      output: "number",
    },
    BigInteger: {
      input: "number",
      output: "number",
    },
    Long: {
      input: "number",
      output: "number",
    },
  },
};
