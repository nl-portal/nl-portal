import type { CodegenConfig } from "@graphql-codegen/cli";

const config: CodegenConfig = {
  overwrite: true,
  noSilentErrors: true,
  schema: "http://localhost:8080/graphql",
  documents: [
    "./src/{fragments,mutations,queries}/**/*.{ts,tsx}",
    // Excluded: generate-ogone-payment references a mutation that is absent when the
    // payment-ogone module is disabled (NLPORTAL_CONFIG_PAYMENT_OGONE_ENABLED=false).
    "!./src/mutations/generate-ogone-payment.ts",
  ],
  generates: {
    "src/generated/graphql.ts": {
      plugins: ["typescript", "typescript-operations", "typed-document-node"],
    },
  },
};

export default config;
