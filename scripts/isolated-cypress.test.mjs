import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  guardCypressNodeSetup,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
  SUPPORTED_ISOLATED_MCP_SPEC,
} from './isolated-cypress.mjs'
import {
  assertRefusesBeforeReset,
  cypressArgv,
  isolatedCypressOpts,
  isolatedCypressSpec,
  isolatedOrigin,
  supportedConfig,
} from './isolated-cypress-test-helpers.mjs'
import {
  completeIsolatedConfig,
  startLiveOwner,
} from './sut-isolated-fixtures.mjs'
import { beginSutOwnerShutdown } from './sut-owner.mjs'

test('selections containing unsupported isolated Cypress specs refuse before reset', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  // A wholly-ignored file is permanently outside admission; a resource-dependent
  // file not yet admitted by its owning slice is also unsupported here.
  const unsupported = 'e2e_test/features/book_reading/epub_book.feature'

  for (const argv of [
    cypressArgv(unsupported),
    [
      'node',
      'cypress',
      'run',
      '--spec',
      `${SUPPORTED_ISOLATED_CYPRESS_SPEC},${unsupported}`,
    ],
    ['node', 'cypress', 'run'],
  ]) {
    await assertRefusesBeforeReset(
      () =>
        guardCypressNodeSetup(
          checkout.root,
          { specPattern: 'e2e_test/features/**/*.feature' },
          { argv }
        ),
      isolatedCypressSpec
    )
  }
})

test('CLI glob spec selection is refused in before:run before reset', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const listeners = {}
  const config = {
    specPattern: 'e2e_test/features/**/*.feature',
    baseUrl: 'http://localhost:5173',
  }
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({
      argv: ['node', 'cypress', 'run'],
      on: (event, fn) => {
        listeners[event] = fn
      },
    })
  )
  t.after(() => isolated.release())
  assert.equal(config.baseUrl, isolatedOrigin)
  await assertRefusesBeforeReset(
    () =>
      listeners['before:run']({
        specs: [
          { relative: SUPPORTED_ISOLATED_CYPRESS_SPEC },
          {
            relative: 'e2e_test/features/book_reading/epub_book.feature',
          },
        ],
      }),
    isolatedCypressSpec
  )
})

test('MCP spec is allowlisted as a single isolated spec', async (t) => {
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(SUPPORTED_ISOLATED_MCP_SPEC),
    true
  )
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const config = supportedConfig(
    'http://localhost:5173',
    SUPPORTED_ISOLATED_MCP_SPEC
  )
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({ argv: cypressArgv(SUPPORTED_ISOLATED_MCP_SPEC) })
  )
  t.after(() => isolated.release())
  assert.equal(config.baseUrl, isolatedOrigin)
})

test('application-only active specs are admitted; resource-dependent and ignored files are not', () => {
  // Representative admitted application-only spec (the live proof target).
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(
      'e2e_test/features/note_creation_and_update/note_creation.feature'
    ),
    true
  )
  // Excluded groups: wholly-ignored, CLI (not yet this slice), OpenAI mock,
  // Wikidata mock, live OpenAI — none admitted by slice 1.
  for (const excluded of [
    'e2e_test/features/book_reading/epub_book.feature',
    'e2e_test/features/cli/cli_notebook_clone.feature',
    'e2e_test/features/ai_generated_recall_questions/question_contest.feature',
    'e2e_test/features/wikidata/note_create_with_wikidata_id.feature',
    'e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature',
  ]) {
    assert.equal(
      SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(excluded),
      false,
      `${excluded} must not be admitted by slice 1`
    )
  }
})

test('supported isolated Cypress sets origin before reset and serializes the runner lease', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  const config = supportedConfig()
  const listeners = {}
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({
      on: (event, fn) => {
        listeners[event] = fn
      },
    })
  )
  t.after(() => isolated.release())
  assert.equal(config.baseUrl, isolatedOrigin)

  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        checkout.root,
        supportedConfig(),
        isolatedCypressOpts()
      ),
    /duplicate runner|already using this checkout/i
  )

  await listeners['after:run']()
  const config2 = supportedConfig()
  const again = await guardCypressNodeSetup(
    checkout.root,
    config2,
    isolatedCypressOpts()
  )
  t.after(() => again.release())
  assert.equal(config2.baseUrl, isolatedOrigin)
})

test('conflicting Cypress origin refuses before reset; matching origin remains usable', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        checkout.root,
        supportedConfig(),
        isolatedCypressOpts({
          env: { CYPRESS_baseUrl: 'http://localhost:5173' },
        })
      ),
    /Conflicting CYPRESS_baseUrl/
  )

  const config = supportedConfig(isolatedOrigin)
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({ env: { CYPRESS_baseUrl: isolatedOrigin } })
  )
  t.after(() => isolated.release())
  assert.equal(config.baseUrl, isolatedOrigin)
})

test('isolated Cypress without a live owner refuses before reset', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        checkout.root,
        supportedConfig(),
        isolatedCypressOpts()
      ),
    /verified live SUT owner/
  )
})

test('owner shutdown refuses a new Cypress runner lease before reset', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  await beginSutOwnerShutdown(checkout.root)
  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        checkout.root,
        supportedConfig(),
        isolatedCypressOpts()
      ),
    /shutting down/
  )
})
