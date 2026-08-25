const minorConfig = require("./.ncurc.minor");

// Keep ESLint on v9, since a lot of the eslint plugins are not yet compatible with v10.
// Typescript v7 is not yet supported by @typescript-eslint plugins, so we keep it on v6 for now.

module.exports = {
  ...minorConfig,
  reject: [...minorConfig.reject, "eslint", "typescript"],
  target: "latest",
};
