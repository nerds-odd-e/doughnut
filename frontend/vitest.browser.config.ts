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
import { VueRouterAutoImports } from "unplugin-vue-router"
import VueRouter from "unplugin-vue-router/vite"
import tsconfigPaths from "vite-tsconfig-paths"

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
    setupFiles: ["./tests/setupVitest.browser.ts"],
    include: ["./**/*.browser.spec.{ts,tsx}"],
    exclude: [
      "packages/template/*",
      "node_modules/**/*.spec.js",
      "node_modules/**/*.test.js",
      "node_modules/**/test.js",
      "**/setupVitest.browser.ts",
      "**/setup*.ts",
    ],
    browser: {
      enabled: true,
      headless: process.env.CI === "true",
      provider: playwright(),
      screenshotDirectory: "vitest-test-results",
      instances: [{ browser: "chromium" }],
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
    VueRouter(),
    vueJsx(),
    AutoImport({
      imports: ["vue", "vue-router", "vitest", VueRouterAutoImports],
      dts: false, // Disable dts generation during tests
    }),
    Components({}),
  ],
})

export default config
