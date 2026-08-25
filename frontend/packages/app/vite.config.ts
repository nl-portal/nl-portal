/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  build: {
    chunkSizeWarningLimit: 2048,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name(id) {
                if (id.includes("@formio")) {
                  return "formio";
                }

                if (id.includes("@gemeente-denhaag")) {
                  return "gemeente-denhaag";
                }

                if (id.includes("@nl-portal")) {
                  return "nl-portal";
                }

                return null;
              },
            },
          ],
        },
      },
    },
    outDir: "build",
  },
  html: {
    cspNonce: "##NL_PORTAL_NONCE##",
  },
  plugins: [react()],
  test: {
    css: true,
    environment: "jsdom",
    globals: true,
  },
  ssr: { noExternal: true },
});
