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
  // Active CLI workflows are admitted by slice 2 (selected-origin routing,
  // temporary config/install/clone dirs, checkout-local bundles).
  for (const cliSpec of [
    'e2e_test/features/cli/cli_notebook_web_created_note.feature',
    'e2e_test/features/cli/cli_install_and_run.feature',
    'e2e_test/features/cli/cli_notebook_clone.feature',
    'e2e_test/features/cli/cli_notebook_existing_note_edits.feature',
    'e2e_test/features/cli/cli_notebook_folder_relocation.feature',
  ]) {
    assert.equal(
      SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(cliSpec),
      true,
      `${cliSpec} must be admitted by slice 2`
    )
  }
  // Excluded groups: wholly-ignored (including CLI) and live OpenAI — none
  // admitted by slices 1–3 or 6. Active OpenAI-mock features are admitted by
  // slice 3 and Wikidata-mock features by slice 6 through the same registry.
  for (const excluded of [
    'e2e_test/features/book_reading/epub_book.feature',
    'e2e_test/features/cli/cli_access_token.feature',
    'e2e_test/features/cli/cli_gmail.feature',
    'e2e_test/features/cli/cli_interactive_mode.feature',
    'e2e_test/features/cli/cli_recall.feature',
    'e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature',
  ]) {
    assert.equal(
      SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(excluded),
      false,
      `${excluded} must not be admitted`
    )
  }
})

test('active Wikidata-mock specs are admitted by slice 6 with a private-mock requirement', () => {
  // The four current Wikidata feature files are admitted through the single
  // registry, each declaring requiresPrivateWikidataMock. The mixed file
  // associate_wikidata.feature is admitted as a file; its real-service
  // scenario preserves existing opt-in/credential filtering at scenario
  // selection — the registry authorizes the private mock for the file, it
  // does not mock real-service scenarios.
  for (const wikidataSpec of [
    'e2e_test/features/wikidata/associate_wikidata.feature',
    'e2e_test/features/wikidata/associate_wikidata_location_entries.feature',
    'e2e_test/features/wikidata/associate_wikidata_person_entries.feature',
    'e2e_test/features/wikidata/note_create_with_wikidata_id.feature',
  ]) {
    assert.equal(
      SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(wikidataSpec),
      true,
      `${wikidataSpec} must be admitted by slice 6`
    )
  }
  // The mixed file's real-service sibling (live OpenAI) stays excluded.
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(
      'e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature'
    ),
    false
  )
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
