import { defineConfig } from "vite";
import { fileURLToPath, URL } from "url";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import checker from 'vite-plugin-checker'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'

export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        charset: false
      }
    }
  },
  plugins: [
    checker({
      vueTsc: true,
      eslint: {
        lintCommand: 'eslint "./src/**/*.{vue,ts,tsx}" "./tests/**/*.ts"',
      }
    }),
    vue({
      reactivityTransform: true,
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    vueJsx(),
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'vitest'
      ],
      dts: true, // generate TypeScript declaration
    }),
    Components()
  ],
  server: {
    proxy: {
      "/api": "http://localhost:9081",
      "/images": "http://localhost:9081",
      "/login": "http://localhost:9081",
      "/logout": "http://localhost:9081",
      "/users/identify": "http://localhost:9081",
      "/testability": "http://localhost:9081",
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    },
  },
  base: "/",
  build: {
    outDir: "../backend/src/main/resources/static",
    sourcemap: true,
    rollupOptions: {
      input: {
        main: fileURLToPath(new URL("index.html", import.meta.url))
      },
    },
  },
});
