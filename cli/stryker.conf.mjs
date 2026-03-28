// @ts-check
/** @type {import('@stryker-mutator/api/core').PartialStrykerOptions} */
const config = {
  packageManager: 'pnpm',
  plugins: ['@stryker-mutator/vitest-runner'],
  reporters: ['clear-text', 'progress'],
  testRunner: 'vitest',
  vitest: {
    /** `related: false` runs the full Vitest suite per mutant so mutants in `markdown.ts` are exercised. */
    configFile: 'vitest.config.ts',
    related: false,
  },
  coverageAnalysis: 'off',
  mutate: ['src/markdown.ts'],
  thresholds: { high: 100, low: 80, break: 0 },
}
export default config
