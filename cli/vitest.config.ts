import { defineConfig } from 'vitest/config'
import { resolve } from 'node:path'

export default defineConfig({
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    exclude: ['node_modules', 'build'],
    globals: false,
    setupFiles: ['tests/setup.ts'],
  },
  resolve: {
    alias: {
      '@generated': resolve(__dirname, '../generated'),
    },
  },
})
