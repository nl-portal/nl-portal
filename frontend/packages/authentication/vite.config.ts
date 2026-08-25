/// <reference types="vitest/config" />
import { resolve } from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import dts from "vite-plugin-dts";
import peerDepsExternal from "rollup-plugin-peer-deps-external";

export default defineConfig({
  plugins: [peerDepsExternal({ includeDependencies: true }), react(), dts()],
  build: {
    lib: {
      entry: resolve(import.meta.dirname, "src/index.ts"),
      name: "authentication",
      fileName: "index",
      formats: ["es"],
    },
    rolldownOptions: {
      output: {
        entryFileNames: "[name].js",
        globals: {
          react: "React",
        },
      },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      exclude: ["**/generated/**", "**/node_modules/**"],
    },
  },
});
