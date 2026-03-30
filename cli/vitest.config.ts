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
    include: ['tests/**/*.test.ts', 'tests/**/*.test.tsx'],
    exclude: ['node_modules', 'build'],
    globals: false,
    setupFiles: ['tests/setup.ts'],
    env: {
      // Enable chalk ANSI colors so Ink renders styled output (e.g. bold / colors)
      // even when stdout is not a real TTY (as in test environments).
      FORCE_COLOR: '1',
      // Override CI=1 set by the nix dev environment. is-in-ci checks `env[key] !== '0'`,
      // so CI='0' makes it evaluate to false for consistent Ink behavior in tests.
      CI: '0',
    },
  },
})
