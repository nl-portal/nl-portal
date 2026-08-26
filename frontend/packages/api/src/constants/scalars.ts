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
      input: "unknown",
      output: "unknown",
    },
    BigInteger: {
      input: "unknown",
      output: "unknown",
    },
    Long: {
      input: "unknown",
      output: "unknown",
    },
  },
};
