import { mergeConfig } from "vite";
import { defineConfig } from "vitest/config";
import viteConfig from "./vite.config";

export default mergeConfig(viteConfig, defineConfig({
  test: {
    exclude: [
      "packages/template/*",
      "node_modules/**/*.spec.js",
      "node_modules/**/*.test.js"
    ],
    globals: true,
    environment: "jsdom",
    "setupFiles": [
      "./tests/setupVitest.js"
    ]
  },
}));
