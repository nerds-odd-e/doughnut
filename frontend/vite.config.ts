/// <reference types="vitest" />
import { URL, fileURLToPath } from 'node:url'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VueRouterAutoImports } from 'unplugin-vue-router'
import VueRouter from 'unplugin-vue-router/vite'
import { defineConfig } from 'vite'
import checker from 'vite-plugin-checker'
import viteCompression from 'vite-plugin-compression'
import tsconfigPaths from 'vite-tsconfig-paths'
import tailwindcss from 'tailwindcss'
import autoprefixer from 'autoprefixer'
import Inspector from 'unplugin-vue-inspector/vite'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@tests': fileURLToPath(new URL('./tests', import.meta.url))
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./tests/setupVitest.js'],
    exclude: [
      'packages/template/*',
      'node_modules/**/*.spec.js',
      'node_modules/**/*.test.js',
      'node_modules/**/test.js',
    ],
  },
  css: {
    devSourcemap: true,
    postcss: {
      plugins: [
        tailwindcss({
          config: './tailwind.config.ts',
        }),
        autoprefixer(),
      ],
    },
  },
  plugins: [
    tsconfigPaths(),
    checker({
      vueTsc: true,
    }),
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
    viteCompression(),
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:9081',
      '/attachments': 'http://localhost:9081',
      '/logout': 'http://localhost:9081',
      '/testability': 'http://localhost:9081',
    },
  },
  base: '/',
  build: {
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
        assetFileNames: (assetInfo) => {
          if (assetInfo.name === 'main.css') return 'assets/main.css';
          return 'assets/[name]-[hash][extname]';
        },
      },
    },
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'quill', 'gsap', 'marked', 'turndown', 'es-toolkit'],
    exclude: ['fsevents']
  }
})
