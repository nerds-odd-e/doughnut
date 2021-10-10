import path from "path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";

// https://vitejs.dev/config/
export default defineConfig({
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
    fs: {
      strict: false,
    },
    proxy: {
      "/api": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
      "/images": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
      "/login": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
      "/logout": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
      "/users/identify": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
      "/testability": {
        target: "http://localhost:9081",
        changeOrigin: true,
        ws: true,
      },
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
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, "index.html"),
      },
    },
  },
});
