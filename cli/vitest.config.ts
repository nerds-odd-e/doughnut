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
  },
})
