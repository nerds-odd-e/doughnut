import { defineConfig } from 'vitest/config'
import { resolve } from 'node:path'

export default defineConfig({
  resolve: {
    alias: {
      '@generated/doughnut-backend-api': resolve(
        __dirname,
        '../packages/generated/doughnut-backend-api'
      ),
    },
  },
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    exclude: ['node_modules', 'build'],
    globals: false,
    setupFiles: ['tests/setup.ts'],
    env: {
      // Enable chalk ANSI colors so Ink renders styled output (e.g. inverse/highlight)
      // even when stdout is not a real TTY (as in test environments).
      FORCE_COLOR: '1',
      // Override CI=1 set by the nix dev environment. is-in-ci checks `env[key] !== '0'`,
      // so CI='0' makes it evaluate to false — Ink then uses log-update cursor-positioning
      // output so the test write spy can capture and replay frame updates correctly.
      CI: '0',
    },
  },
})
