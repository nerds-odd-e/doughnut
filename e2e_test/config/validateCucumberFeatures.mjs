import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { relative, sep } from 'node:path'
import { discoverE2eSpecs } from './e2eSpecDiscovery.mjs'

const require = createRequire(import.meta.url)
const preprocessorRequire = createRequire(
  require.resolve('@badeball/cypress-cucumber-preprocessor')
)
const { generateMessages } = preprocessorRequire('@cucumber/gherkin')
const { IdGenerator, SourceMediaType } =
  preprocessorRequire('@cucumber/messages')

export function validateCucumberFeatures(config) {
  const failures = []
  for (const spec of discoverE2eSpecs(config)) {
    if (!spec.endsWith('.feature')) continue
    const name = relative(config.projectRoot, spec).replaceAll(sep, '/')
    const envelopes = generateMessages(
      readFileSync(spec, 'utf8'),
      name,
      SourceMediaType.TEXT_X_CUCUMBER_GHERKIN_PLAIN,
      {
        includeSource: false,
        includeGherkinDocument: false,
        includePickles: true,
        newId: IdGenerator.incrementing(),
      }
    )
    const parseErrors = envelopes.flatMap(({ parseError }) =>
      parseError ? [parseError.message] : []
    )
    if (parseErrors.length) {
      failures.push(...parseErrors.map((message) => `  ${name}: ${message}`))
    } else if (!envelopes.some(({ pickle }) => pickle)) {
      failures.push(`  ${name}: no scenarios`)
    }
  }

  if (failures.length) {
    throw new Error(`Invalid Gherkin feature files\n${failures.join('\n')}`)
  }
}
