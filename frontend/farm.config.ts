import { defineConfig } from "@farmfe/core";
import { fileURLToPath, URL } from "url";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import checker from 'vite-plugin-checker';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import viteCompression from 'vite-plugin-compression';
import tsconfigPaths from 'vite-tsconfig-paths'

export default defineConfig({
  vitePlugins: [
    tsconfigPaths(),
    vue({
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
    Components(),
    viteCompression()
  ],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/attachments": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/logout": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
      "/testability": {
        target: "http://localhost:9081",
	changeOrigin: true,
      },
    },
  },
  compilation: {
    output: {
      path: "../backend/src/main/resources/static",
    },
    sourcemap: true,
    input: {
      main: fileURLToPath(new URL("index.html", import.meta.url))
    },
  },
});
