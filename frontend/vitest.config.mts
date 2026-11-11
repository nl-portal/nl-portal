import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    projects: [
      "./packages/authentication/vite.config.ts",
      "./packages/localization/vite.config.ts",
      "./packages/api/vite.config.ts",
      "./packages/user-interface/vite.config.ts",
      "./packages/app/vite.config.ts",
      "./packages/app-dev/vite.config.ts",
    ],
  },
});
