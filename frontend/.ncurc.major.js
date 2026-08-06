const minorConfig = require("./.ncurc.minor");

// Keep ESLint on v9, since a lot of the eslint plugins are not yet compatible with v10.

module.exports = {
  ...minorConfig,
  reject: [
    ...minorConfig.reject,
    "eslint",
    "typescript",
    "graphql",
    "@graphql-codegen/cli",
    "@graphql-codegen/client-preset",
    "@graphql-typed-document-node/core",
  ],
  target: "latest",
};
