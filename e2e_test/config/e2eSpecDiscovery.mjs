import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const preprocessorRequire = createRequire(
  require.resolve('@badeball/cypress-cucumber-preprocessor')
)
const { globSync } = preprocessorRequire('glob')

export const e2eSpecPatterns = {
  specPattern: 'e2e_test/features/**/*.feature',
  excludeSpecPattern: [
    '**/*.{js,ts}',
    '**/__snapshots__/*',
    '**/__image_snapshots__/*',
  ],
}

export function discoverE2eSpecs(config) {
  // Match the preprocessor's getSpecs, including basename-only exclusions.
  const ignore = [config.excludeSpecPattern]
    .flat()
    .map((pattern) => (pattern.includes('/') ? pattern : `**/${pattern}`))
  if (!ignore.some((pattern) => pattern.includes('node_modules'))) {
    ignore.push('**/node_modules/**')
  }
  return globSync(config.specPattern, {
    cwd: config.projectRoot,
    ignore,
    absolute: true,
    nodir: true,
  }).sort()
}
