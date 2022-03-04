import path from "path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";

export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        charset: false
      }
    }
  },
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    vueJsx(),
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
    alias: [
      {
        find: "@",
        replacement: path.resolve(__dirname, "src"),
      },
    ],
  },
  base: "/",
  build: {
    outDir: "../backend/src/main/resources/static",
    sourcemap: true,
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, "index.html"),
      },
    },
  },
});
