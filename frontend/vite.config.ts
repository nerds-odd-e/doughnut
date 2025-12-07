/// <reference types="vitest" />
/// <reference types="vite/client" />
import { URL, fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import autoprefixer from 'autoprefixer'
import tailwindcss from 'tailwindcss'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import Inspector from 'unplugin-vue-inspector/vite'
import { VueRouterAutoImports } from 'unplugin-vue-router'
import VueRouter from 'unplugin-vue-router/vite'
import checker from 'vite-plugin-checker'
import tsconfigPaths from 'vite-tsconfig-paths'

// Check if we're running tests - Vitest sets process.env.VITEST
// This is the official and most reliable way to detect test mode
const isTest = process.env.VITEST !== undefined

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
    // Performance optimizations
    pool: 'threads', // Use threads pool for better performance
    // Disable coverage collection during tests (not needed for unit tests)
    coverage: {
      enabled: false,
    },
    // Optimize file parallelism
    fileParallelism: true,
    // Keep isolation enabled for safety (tests should be isolated)
    isolate: true,
  },
  css: {
    // Disable sourcemaps during tests (saves ~0.3s)
    devSourcemap: !isTest,
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
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => /^x-/.test(tag),
        },
      },
    }),
    // Disable Inspector during tests (saves ~0.3s)
    ...(isTest ? [] : [
      Inspector({
        launchEditor: 'cursor',
      }),
    ]),
    VueRouter(),
    vueJsx(),
    AutoImport({
      imports: ['vue', 'vue-router', 'vitest', VueRouterAutoImports],
      // Disable dts generation during tests (saves ~0.3s)
      dts: !isTest,
    }),
    Components({}),
    // Disable checker plugin during test runs to speed up tests
    // Linting and type checking should be done separately, not during test execution
    ...(isTest ? [] : [
      checker({
        typescript: true,
        vueTsc: true, // This will check Vue templates
        biome: true,  // This will use Biome for linting
        overlay: false, // Disable overlay to prevent blocking e2e tests - errors still show in terminal
      }),
    ]),
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
