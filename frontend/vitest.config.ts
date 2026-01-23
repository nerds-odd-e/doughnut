/// <reference types="vitest" />
/// <reference types="vite/client" />
import { URL, fileURLToPath } from "node:url"
import { defineConfig } from "vitest/config"

import vue from "@vitejs/plugin-vue"
import vueJsx from "@vitejs/plugin-vue-jsx"
import { playwright } from "@vitest/browser-playwright"
import autoprefixer from "autoprefixer"
import tailwindcss from "tailwindcss"
import AutoImport from "unplugin-auto-import/vite"
import Components from "unplugin-vue-components/vite"
import Inspector from "unplugin-vue-inspector/vite"
import { VueRouterAutoImports } from "unplugin-vue-router"
import VueRouter from "unplugin-vue-router/vite"
import checker from "vite-plugin-checker"
import tsconfigPaths from "vite-tsconfig-paths"

// Check if we're running tests - Vitest sets process.env.VITEST
// This is the official and most reliable way to detect test mode
const isTest = process.env.VITEST !== undefined

const config = defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@tests": fileURLToPath(new URL("./tests", import.meta.url)),
      // Browser Mode needs the bundler build of Vue for template compilation
      vue: "vue/dist/vue.esm-bundler.js",
    },
  },
  optimizeDeps: {
    include: [
      "vue",
      "vue-router",
      "@testing-library/vue",
      "es-toolkit",
      "vue-content-loader",
      "gsap",
      "mini-debounce",
      "quill",
      "file-saver",
      "turndown",
      "vue-toastification",
      "marked",
      "turndown-plugin-gfm",
    ],
  },
  test: {
    globals: true,
    environment: "node", // Browser mode uses 'node' environment
    setupFiles: ["./tests/setupVitest.ts"],
    include: ["./**/*.spec.{ts,tsx}"],
    exclude: [
      "packages/template/*",
      "node_modules/**/*.spec.js",
      "node_modules/**/*.test.js",
      "node_modules/**/test.js",
      "**/setupVitest.ts",
      "**/setup*.ts",
    ],
    browser: {
      enabled: true,
      headless: process.env.CI === "true",
      provider: playwright(),
      screenshotDirectory: "vitest-test-results",
      instances: [{ browser: "chromium" }],
    },
    // Disable coverage collection during tests (not needed for unit tests)
    coverage: {
      enabled: false,
    },
  },
  css: {
    postcss: {
      plugins: [
        tailwindcss({
          config: "./tailwind.config.ts",
        }),
        autoprefixer(),
      ],
    },
  },
  plugins: [
    tsconfigPaths(),
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    // Disable Inspector during tests (saves ~0.3s)
    ...(isTest
      ? []
      : [
          Inspector({
            launchEditor: "cursor",
          }),
        ]),
    VueRouter(),
    vueJsx(),
    AutoImport({
      imports: ["vue", "vue-router", "vitest", VueRouterAutoImports],
      dts: false, // Disable dts generation during tests
    }),
    Components({}),
    // Disable checker plugin during test runs to speed up tests
    // Linting and type checking should be done separately, not during test execution
    ...(isTest
      ? []
      : [
          checker({
            typescript: true,
            vueTsc: true, // This will check Vue templates
            biome: true, // This will use Biome for linting
            overlay: false, // Disable overlay to prevent blocking e2e tests - errors still show in terminal
          }),
        ]),
  ],
})

export default config
