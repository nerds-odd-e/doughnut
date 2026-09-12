import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
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
import { assertSupportedIsolatedCypressSpecs } from './isolated-cypress-spec-selection.mjs'

const REPO_ROOT = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)
const FEATURES_DIR = path.join(REPO_ROOT, 'e2e_test', 'features')

/** Known shared-resource families declared by active feature files. */
const KNOWN_MOCK_TAG_FAMILIES = new Set([
  '@usingMockedOpenAiService',
  '@usingMockedWikidataService',
])
const KNOWN_REAL_SERVICE_TAGS = new Set([
  '@usingRealOpenAiService',
  '@usingRealWikidataService',
])
/** Tags that appear only in wholly-ignored files and are not active families. */
const IGNORED_ONLY_RESOURCE_TAGS = new Set(['@usingMockedGoogleService'])

function listFeatureFiles(dir) {
  return readdirSync(dir, { recursive: true })
    .filter((entry) => String(entry).endsWith('.feature'))
    .map((entry) => path.join(dir, String(entry)))
    .sort()
}

function relativeSpec(absolute) {
  return path.relative(REPO_ROOT, absolute).replace(/\\/g, '/')
}

/** Extract Feature-level tags (tags on lines before the `Feature:` line). */
function featureLevelTags(content) {
  const lines = content.split(/\r?\n/)
  const tags = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('Feature:')) break
    if (trimmed.startsWith('@')) {
      for (const tag of trimmed.split(/\s+/)) {
        if (tag.startsWith('@')) tags.push(tag)
      }
    }
  }
  return tags
}

/** Extract all tags (Feature-level and scenario-level) from a feature file. */
function allTags(content) {
  const tags = new Set()
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (trimmed.startsWith('@')) {
      for (const tag of trimmed.split(/\s+/)) {
        if (tag.startsWith('@')) tags.add(tag)
      }
    }
  }
  return tags
}

function readFeature(absolute) {
  return readFileSync(absolute, 'utf8')
}

/** Assess the current feature inventory from the filesystem (not a hardcoded count). */
function assessInventory() {
  const all = listFeatureFiles(FEATURES_DIR)
  const byRel = new Map()
  const whollyIgnored = []
  const active = []
  for (const absolute of all) {
    const rel = relativeSpec(absolute)
    const content = readFeature(absolute)
    const fTags = featureLevelTags(content)
    byRel.set(rel, {
      absolute,
      content,
      featureTags: fTags,
      allTags: allTags(content),
    })
    if (fTags.includes('@ignore')) {
      whollyIgnored.push(rel)
    } else {
      active.push(rel)
    }
  }
  return { all: all.map(relativeSpec), byRel, whollyIgnored, active }
}

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
  await startLiveOwner(checkout.root, t)
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
  await startLiveOwner(checkout.root, t)
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
  await startLiveOwner(checkout.root, t)
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
  await startLiveOwner(checkout.root, t)
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
  await startLiveOwner(checkout.root, t)
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

// --- Slice 9: complete active-inventory coverage / migration-contract reconciliation ---

test('active inventory reconciles with the single registry: every active file is admitted or is a live-provider file; wholly-ignored files are not admitted', () => {
  const { byRel, whollyIgnored, active } = assessInventory()
  const admitted = new Set(SUPPORTED_ISOLATED_CYPRESS_SPECS)
  // Live-provider files preserve existing external-service/credential behavior:
  // identified by a Feature-level real-service tag, not by filename.
  const liveProviderFiles = active.filter((rel) =>
    byRel.get(rel).featureTags.some((tag) => KNOWN_REAL_SERVICE_TAGS.has(tag))
  )
  const liveProviderSet = new Set(liveProviderFiles)

  // Every active file is either admitted by the registry or a live-provider file.
  for (const rel of active) {
    assert.ok(
      admitted.has(rel) || liveProviderSet.has(rel),
      `${rel} is active but neither admitted by the registry nor a live-provider file`
    )
  }
  // No wholly-ignored file is admitted.
  for (const rel of whollyIgnored) {
    assert.equal(
      admitted.has(rel),
      false,
      `${rel} is wholly-ignored and must not be admitted`
    )
  }
  // No live-provider file is admitted (preserves real-service behavior).
  for (const rel of liveProviderFiles) {
    assert.equal(
      admitted.has(rel),
      false,
      `${rel} is a live-provider file and must not be admitted`
    )
  }
  // The registry admits exactly the active files minus live-provider files.
  const expectedAdmitted = new Set(
    active.filter((rel) => !liveProviderSet.has(rel))
  )
  assert.deepEqual(
    [...admitted].sort(),
    [...expectedAdmitted].sort(),
    'registry admitted set must match active files minus live-provider files'
  )
})

test('admitted files resource groups match their declared tags, including scenario-level tags and mixed files', () => {
  const { byRel, active } = assessInventory()
  const admitted = new Set(SUPPORTED_ISOLATED_CYPRESS_SPECS)
  for (const rel of active) {
    if (!admitted.has(rel)) continue
    const tags = byRel.get(rel).allTags
    const reqs = assertSupportedIsolatedCypressSpecs([rel])
    const declaresOpenAiMock = tags.has('@usingMockedOpenAiService')
    const declaresWikidataMock = tags.has('@usingMockedWikidataService')
    assert.equal(
      reqs.requiresPrivateOpenAiMock,
      declaresOpenAiMock,
      `${rel}: registry OpenAI-mock requirement must match @usingMockedOpenAiService tag`
    )
    assert.equal(
      reqs.requiresPrivateWikidataMock,
      declaresWikidataMock,
      `${rel}: registry Wikidata-mock requirement must match @usingMockedWikidataService tag`
    )
  }
})

test('no new shared-resource family is discovered in the active inventory', () => {
  const { byRel, active } = assessInventory()
  const knownFamilies = new Set([
    ...KNOWN_MOCK_TAG_FAMILIES,
    ...KNOWN_REAL_SERVICE_TAGS,
    ...IGNORED_ONLY_RESOURCE_TAGS,
  ])
  for (const rel of active) {
    const tags = byRel.get(rel).allTags
    for (const tag of tags) {
      if (tag.startsWith('@usingMocked') || tag.startsWith('@usingReal')) {
        assert.ok(
          knownFamilies.has(tag),
          `${rel}: discovered unknown resource family ${tag} — stop for scope review`
        )
      }
    }
  }
})

test('mixed Wikidata file is admitted at the routing/filter boundary; its real-service scenario preserves existing URL policy without a mock', () => {
  const mixedSpec = 'e2e_test/features/wikidata/associate_wikidata.feature'
  const { byRel } = assessInventory()
  const info = byRel.get(mixedSpec)
  assert.ok(info, `${mixedSpec} must exist in the inventory`)
  // The file has both mocked and real-service scenarios.
  assert.equal(
    info.allTags.has('@usingMockedWikidataService'),
    true,
    `${mixedSpec} must have a @usingMockedWikidataService scenario`
  )
  assert.equal(
    info.allTags.has('@usingRealWikidataService'),
    true,
    `${mixedSpec} must have a @usingRealWikidataService scenario`
  )
  // The registry admits the file and declares a Wikidata-mock requirement.
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(mixedSpec),
    true,
    `${mixedSpec} must be admitted by the registry`
  )
  const reqs = assertSupportedIsolatedCypressSpecs([mixedSpec])
  assert.equal(reqs.requiresPrivateWikidataMock, true)
  assert.equal(reqs.requiresPrivateOpenAiMock, false)
  // The file is NOT a live-provider file: it does not have a Feature-level
  // real-service tag. The real-service scenario is scenario-level, so the
  // file is admitted and Cucumber's scenario tag selection chooses mocked
  // versus real service URLs per scenario.
  assert.equal(
    info.featureTags.some((tag) => KNOWN_REAL_SERVICE_TAGS.has(tag)),
    false,
    `${mixedSpec} must not have a Feature-level real-service tag`
  )
})

test('live OpenAI file is refused at the routing/filter boundary; it preserves existing external-service/credential behavior', () => {
  const liveSpec =
    'e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature'
  const { byRel } = assessInventory()
  const info = byRel.get(liveSpec)
  assert.ok(info, `${liveSpec} must exist in the inventory`)
  // The file has a Feature-level @usingRealOpenAiService tag.
  assert.equal(
    info.featureTags.includes('@usingRealOpenAiService'),
    true,
    `${liveSpec} must have a Feature-level @usingRealOpenAiService tag`
  )
  // The file is NOT admitted by the registry — preserves real-service behavior.
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(liveSpec),
    false,
    `${liveSpec} must not be admitted (preserves real-service behavior)`
  )
  // Selecting it in isolation is refused at the routing/filter boundary
  // without paid/live requests.
  assert.throws(
    () => assertSupportedIsolatedCypressSpecs([liveSpec]),
    /only supports selections/
  )
})
