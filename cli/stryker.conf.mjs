// @ts-check
/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
const config = {
  packageManager: 'pnpm',
  plugins: ['@stryker-mutator/vitest-runner'],
  reporters: ['clear-text', 'progress'],
  testRunner: 'vitest',
  vitest: {
    /** Full Vitest include list; `related: false` runs the suite per mutant (no direct-import test for this module). */
    configFile: 'vitest.config.ts',
    related: false,
  },
  coverageAnalysis: 'off',
  mutate: ['src/interactions/selectListInteraction.ts'],
  thresholds: { high: 100, low: 80, break: 0 },
}
export default config
