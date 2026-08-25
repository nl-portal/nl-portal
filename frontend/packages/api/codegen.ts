import type { CodegenConfig } from "@graphql-codegen/cli";
import scalarConfig from "./src/constants/scalars";

const config: CodegenConfig = {
  overwrite: true,
  noSilentErrors: true,
  schema: "http://localhost:8080/graphql",
  documents: "./src/{fragments,mutations,queries}/**/*.{ts,tsx}",
  generates: {
    "src/generated/types.ts": {
      plugins: ["typescript"],
      config: {
        ...scalarConfig,
        useTypeImports: true,
      },
    },
    "src/generated/graphql.ts": {
      plugins: ["typescript-operations", "typed-document-node"],
      config: {
        ...scalarConfig,
        enumType: "native",
        importSchemaTypesFrom: "./src/generated/types.ts",
        useTypeImports: true,
      },
    },
  },
};

export default config;
