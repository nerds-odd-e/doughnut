// @ts-check
/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
const config = {
  packageManager: 'pnpm',
  plugins: ['@stryker-mutator/vitest-runner'],
  reporters: ['clear-text', 'progress'],
  testRunner: 'vitest',
  vitest: {
    /** Direct import tests so instrumented selectListInteraction.ts is executed */
    configFile: 'vitest.stryker.config.ts',
    related: false,
  },
  coverageAnalysis: 'off',
  mutate: ['src/interactions/selectListInteraction.ts'],
  thresholds: { high: 100, low: 80, break: 0 },
}
export default config
