/// <reference types="vitest" />
/// <reference types="vite/client" />
import { URL, fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VueRouterAutoImports } from 'unplugin-vue-router'
import VueRouter from 'unplugin-vue-router/vite'
import tsconfigPaths from 'vite-tsconfig-paths'
import tailwindcssPlugin from '@tailwindcss/postcss'
import autoprefixer from 'autoprefixer'
import Inspector from 'unplugin-vue-inspector/vite'

const config = defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@tests': fileURLToPath(new URL('./tests', import.meta.url)),
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    environmentOptions: {
      jsdom: {
        url: 'http://localhost/',
      },
    },
    setupFiles: ['./tests/setupVitest.js'],
    exclude: [
      'packages/template/*',
      'node_modules/**/*.spec.js',
      'node_modules/**/*.test.js',
      'node_modules/**/test.js',
    ],
    fakeTimers: {
      toFake: [
        "Date",
        "setTimeout",
        "clearTimeout",
        "setInterval",
        "clearInterval",
      ],
    },
  },
  css: {
    devSourcemap: true,
    postcss: {
      plugins: [
        tailwindcssPlugin(),
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
    Inspector({
      launchEditor: 'cursor',
    }),
    VueRouter(),
    vueJsx(),
    AutoImport({
      imports: ['vue', 'vue-router', 'vitest', VueRouterAutoImports],
      dts: true,
    }),
    Components({}),
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:9081',
      '/attachments': 'http://localhost:9081',
      '/logout': 'http://localhost:9081',
      '/testability': 'http://localhost:9081',
    },
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'quill', 'gsap', 'marked', 'turndown', 'es-toolkit'],
    exclude: ['fsevents'],
    force: true
  },
  base: '/',
  build: {
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      },
    },
    reportCompressedSize: false,
    outDir: '../backend/src/main/resources/static/',
    chunkSizeWarningLimit: 1000,
    sourcemap: true,
    rollupOptions: {
      input: {
        main: fileURLToPath(new URL('index.html', import.meta.url)),
      },
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          'ui-vendor': ['quill', 'gsap', 'marked', 'turndown']
        },
        assetFileNames: ({ name }) => {
          if (name === 'main.css') return 'assets/main.css';
          return 'assets/[name]-[hash][extname]';
        },
      },
    },
  },
})

export default config
