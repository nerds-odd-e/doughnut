import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { after, test } from 'node:test'
import { validateCucumberFeatures } from './validateCucumberFeatures.mjs'

const root = mkdtempSync(join(tmpdir(), 'donut-gherkin-validation-'))
after(() => rmSync(root, { recursive: true, force: true }))

const features = join(root, 'features')
mkdirSync(features)
mkdirSync(join(root, 'node_modules'))

function writeFeature(relativePath, contents) {
  const absolute = join(root, relativePath)
  mkdirSync(join(absolute, '..'), { recursive: true })
  writeFileSync(absolute, contents)
}

writeFeature(
  'features/broken.feature',
  `Feature: Broken syntax
  Scenario: Broken
    Given an accepted value "x"
    stray text
`
)
writeFeature(
  'features/empty.feature',
  `Feature: Empty examples
  Scenario: Empty
    Given an accepted value "<value>"
    Examples:
`
)
writeFeature(
  'features/ok.feature',
  `Feature: Ok
  Scenario: Ok
    Given an accepted value "x"
`
)
writeFeature(
  'features/skip.feature',
  `Feature: Skipped invalid
  Scenario: Broken
    Given an accepted value "x"
    stray text
`
)
writeFeature(
  'node_modules/x.feature',
  `Feature: Nested invalid
  Scenario: Broken
    Given an accepted value "x"
    stray text
`
)

function config(overrides = {}) {
  return {
    projectRoot: root,
    specPattern: 'features/**/*.feature',
    excludeSpecPattern: 'skip.feature',
    ...overrides,
  }
}

test('reports a parse error with line and text', () => {
  assert.throws(
    () =>
      validateCucumberFeatures(
        config({ excludeSpecPattern: ['skip.feature', 'empty.feature'] })
      ),
    (error) => {
      assert.match(error.message, /^Invalid Gherkin feature files\n/)
      assert.match(
        error.message,
        /features\/broken\.feature: \(4:5\).*stray text/
      )
      assert.doesNotMatch(error.message, /empty\.feature/)
      return true
    }
  )
})

test('reports a feature with no scenarios', () => {
  assert.throws(
    () =>
      validateCucumberFeatures(
        config({ excludeSpecPattern: ['skip.feature', 'broken.feature'] })
      ),
    (error) => {
      assert.match(error.message, /^Invalid Gherkin feature files\n/)
      assert.match(error.message, /features\/empty\.feature: no scenarios/)
      assert.doesNotMatch(error.message, /broken\.feature/)
      return true
    }
  )
})

test('ignores a basename-only excludeSpecPattern', () => {
  assert.throws(
    () => validateCucumberFeatures(config()),
    (error) => {
      assert.match(error.message, /features\/broken\.feature/)
      assert.match(error.message, /features\/empty\.feature: no scenarios/)
      assert.doesNotMatch(error.message, /skip\.feature/)
      return true
    }
  )
})

test('ignores feature files under node_modules', () => {
  assert.throws(
    () =>
      validateCucumberFeatures(
        config({
          specPattern: '**/*.feature',
        })
      ),
    (error) => {
      assert.doesNotMatch(error.message, /node_modules/)
      return true
    }
  )
})

test('accepts a valid feature set', () => {
  assert.doesNotThrow(() =>
    validateCucumberFeatures(
      config({
        excludeSpecPattern: ['skip.feature', 'broken.feature', 'empty.feature'],
      })
    )
  )
})
