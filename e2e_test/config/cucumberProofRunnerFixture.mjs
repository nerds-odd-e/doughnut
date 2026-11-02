import { createRequire } from 'node:module'
import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)

export function writeRunnerFixture(root) {
  const preprocessor = require.resolve(
    '@badeball/cypress-cucumber-preprocessor'
  )
  const esbuild = require.resolve(
    '@badeball/cypress-cucumber-preprocessor/esbuild'
  )
  const bundler = require.resolve('@bahmutov/cypress-esbuild-preprocessor')
  mkdirSync(join(root, 'features'))
  mkdirSync(join(root, 'outside'))
  writeFileSync(
    join(root, 'features/normal.feature'),
    `Feature: Observations
  Scenario: Retry succeeds
    Given a retry succeeds
  @selected
  Scenario Outline: Parsed <value>
    Given an accepted value "<value>"
    Examples:
      | value |
      | a\\|b |
      | back\\\\slash |
  Scenario: Permanent failure
    Given an intentional failure
  Scenario: Cucumber hook fails
    Given an accepted value "hook"
`
  )
  writeFileSync(
    join(root, 'features/hooks.feature'),
    `Feature: Raw hooks
  Scenario: Raw hook fails
    Given an accepted value "never"
  Scenario: Skipped after raw hook
    Given an accepted value "never"
`
  )
  writeFileSync(
    join(root, 'outside/outside.feature'),
    `Feature: Outside scope
  Scenario: Outside passes
    Given an accepted value "outside"
`
  )
  writeFileSync(
    join(root, 'steps.cjs'),
    `
const { Given, After } = require('@badeball/cypress-cucumber-preprocessor')
let attempts = 0
Given('a retry succeeds', () => { attempts++; expect(attempts, 'retry count').to.be.greaterThan(1) })
Given('an accepted value {string}', value => { expect(value).to.be.a('string') })
Given('an intentional failure', () => { throw new Error('proof fixture intentional failure') })
After(({ pickle }) => { if (pickle.name === 'Cucumber hook fails') throw new Error('proof fixture Cucumber hook failure') })
beforeEach(function () { if (this.currentTest.title === 'Raw hook fails') throw new Error('proof fixture raw hook failure') })
`
  )
  const adapter = new URL('./cucumberProof.mjs', import.meta.url).href
  const composer = new URL('./composeCypressPluginEvents.mjs', import.meta.url)
    .href
  writeFileSync(
    join(root, 'cypress.config.cjs'),
    `
const fs = require('node:fs')
const { addCucumberPreprocessorPlugin } = require(${JSON.stringify(preprocessor)})
const { createEsbuildPlugin } = require(${JSON.stringify(esbuild)})
const createBundler = require(${JSON.stringify(bundler)})
const path = require('node:path')
module.exports = {
  video: false, screenshotOnRunFailure: false, retries: 1,
  expose: {
    stepDefinitions: [path.join(__dirname, 'steps.cjs')],
    dryRun: process.env.PROOF_MODE === 'dry',
    omitFiltered: true,
    ...(process.env.PROOF_MODE === 'report' ? { messagesEnabled: true, messagesOutput: path.join(__dirname, 'existing.ndjson') } : {}),
  },
  e2e: {
    supportFile: false, specPattern: path.join(__dirname, '**/*.feature'),
    async setupNodeEvents(on, config) {
      const { composeCypressPluginEvents } = await import(${JSON.stringify(composer)})
      on = composeCypressPluginEvents(on)
      on('before:run', () => fs.appendFileSync(path.join(__dirname, 'owners.log'), 'start\\n'))
      on('after:run', () => fs.appendFileSync(path.join(__dirname, 'owners.log'), 'cleanup\\n'))
      if (process.env.PROOF_MODE === 'baseline') await addCucumberPreprocessorPlugin(on, config)
      else {
        const { addCucumberPreprocessorWithProof } = await import(${JSON.stringify(adapter)})
        await addCucumberPreprocessorWithProof(on, config, __dirname)
      }
      on('after:spec', () => fs.appendFileSync(path.join(__dirname, 'owners.log'), 'spec-cleanup\\n'))
      on('file:preprocessor', createBundler({ nodePaths: [${JSON.stringify(fileURLToPath(new URL('../../node_modules', import.meta.url)))}], plugins: [createEsbuildPlugin(config)] }))
      return config
    },
  },
}
`
  )
}
