import { fileURLToPath, URL } from "url";
import { defineConfig } from "vite";
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
          nodeTransforms: [
            (node) => {
              if (process.env.NODE_ENV === "production") {
                //clean up test attrs
                if (node.type === 1 /*NodeTypes.ELEMENT*/) {
                  for (let i = 0; i < node.props.length; i++) {
                    const element = node.props[i]
                    if (element && element.type === 6/*NodeTypes.ATTRIBUTE*/ && element.name === "data-testid") {
                      node.props.splice(i, 1)
                      i--
                    }
                  }
                }
              }
            },
          ]
        },
      },
    }),
    vueJsx(),
    AutoImport({
      imports: [
        'vue',
        'vue-router'
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
