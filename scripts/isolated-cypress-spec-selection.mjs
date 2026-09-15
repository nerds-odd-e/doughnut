import { execSync } from 'node:child_process'
import path from 'node:path'
import {
  ACTIVE_CLI_SPECS,
  APPLICATION_ONLY_ACTIVE_SPECS,
} from './isolated-cypress-active-specs.mjs'

/** No-mock note editing — remains the default allowlisted focused spec. */
export const SUPPORTED_ISOLATED_CYPRESS_SPEC =
  'e2e_test/features/note_creation_and_update/worktree_note_editing.feature'

/** Representative admitted active CLI workflow (web-created note) — clones,
 * creates, pulls, and publishes against the owning worktree's own isolated
 * backend and notebook data. The remaining active CLI workflows are admitted
 * through `ACTIVE_CLI_SPECS` below; this constant stays as the focused
 * representative used by existing CLI origin/command-boundary tests. */
export const SUPPORTED_ISOLATED_CLI_SPEC =
  'e2e_test/features/cli/cli_notebook_web_created_note.feature'

/** MCP search/graph workflow — tool calls reach the owning worktree's
 * isolated backend and notebook data. */
export const SUPPORTED_ISOLATED_MCP_SPEC =
  'e2e_test/features/mcp/mcp_services.feature'

/** OpenAI completion — the representative approved spec that requires a
 * runner-owned private OpenAI mock. The remaining active OpenAI-mock
 * features are admitted through `ACTIVE_OPEN_AI_MOCK_SPECS` below. */
export const SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC =
  'e2e_test/features/ai_generated_content/note_content_completion.feature'

/** Wikidata entity lookup — the representative approved spec that requires a
 * runner-owned private Wikidata mock. The remaining active Wikidata-mock
 * features are admitted through `ACTIVE_WIKIDATA_MOCK_SPECS` below. */
export const SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC =
  'e2e_test/features/wikidata/note_create_with_wikidata_id.feature'

/**
 * Remaining active OpenAI-mock feature files — each declares an existing
 * private-mock requirement. Assessed 2026-09-12 from `e2e_test/features/`.
 * This includes files whose `@usingMockedOpenAiService` tag appears on a
 * scenario rather than on the Feature (`semantic_search`,
 * `property_memory_tracker`, `mcq_management`); the registry's declared
 * requirement is the authority, not the feature filename or tag placement.
 * `note_content_completion.feature` is the already-admitted representative
 * (`SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC`); the remaining active OpenAI-mock
 * features are admitted here. Wikidata-mock and live-OpenAI files belong to
 * later slices and are not admitted here.
 */
const ACTIVE_OPEN_AI_MOCK_SPECS = [
  'e2e_test/features/ai_generated_recall_questions/question_contest.feature',
  'e2e_test/features/book_reading/ai_reorganize_layout.feature',
  'e2e_test/features/bazaar/bazaar_subscription.feature',
  'e2e_test/features/assimilation/note_refinement.feature',
  'e2e_test/features/messages/conversation_about_a_note.feature',
  'e2e_test/features/note_creation_and_update/record_live_audio.feature',
  'e2e_test/features/recall/recall_quiz_ai_question.feature',
  'e2e_test/features/user_admin/manage_ai_models.feature',
  'e2e_test/features/note_view/semantic_search.feature',
  'e2e_test/features/recall/property_memory_tracker.feature',
  'e2e_test/features/note_creation_and_update/mcq_management.feature',
]

/**
 * Active Wikidata-mock feature files — each declares an existing private-mock
 * requirement on the Wikidata service. Assessed 2026-09-12 from
 * `e2e_test/features/wikidata/`. `associate_wikidata.feature` is a mixed
 * file: it also carries a `@usingRealWikidataService` scenario; that
 * real-service scenario preserves its existing opt-in/credential filtering
 * and is NOT mocked — the registry's declared requirement authorizes the
 * private mock for the file, while Cucumber's scenario tag selection still
 * chooses mocked versus real service URLs per scenario. The Wikidata adapter
 * consumes its invocation-owned endpoint instead of the hardcoded 5002/
 * default 2525. `note_create_with_wikidata_id.feature` is the
 * already-admitted representative (`SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC`);
 * the remaining active Wikidata-mock features are admitted here.
 */
const ACTIVE_WIKIDATA_MOCK_SPECS = [
  'e2e_test/features/wikidata/associate_wikidata.feature',
  'e2e_test/features/wikidata/associate_wikidata_location_entries.feature',
  'e2e_test/features/wikidata/associate_wikidata_person_entries.feature',
]

const APPROVED_ISOLATED_CYPRESS_SPECS = [
  { spec: SUPPORTED_ISOLATED_CYPRESS_SPEC },
  { spec: SUPPORTED_ISOLATED_CLI_SPEC },
  ...ACTIVE_CLI_SPECS.map((spec) => ({ spec })),
  { spec: SUPPORTED_ISOLATED_MCP_SPEC },
  {
    spec: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
    requiresPrivateOpenAiMock: true,
  },
  ...ACTIVE_OPEN_AI_MOCK_SPECS.map((spec) => ({
    spec,
    requiresPrivateOpenAiMock: true,
  })),
  {
    spec: SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC,
    requiresPrivateWikidataMock: true,
  },
  ...ACTIVE_WIKIDATA_MOCK_SPECS.map((spec) => ({
    spec,
    requiresPrivateWikidataMock: true,
  })),
  ...APPLICATION_ONLY_ACTIVE_SPECS.map((spec) => ({ spec })),
]

export const SUPPORTED_ISOLATED_CYPRESS_SPECS =
  APPROVED_ISOLATED_CYPRESS_SPECS.map((entry) => entry.spec)

function approvedIsolatedCypressSpec(spec) {
  return APPROVED_ISOLATED_CYPRESS_SPECS.find((entry) => entry.spec === spec)
}

function specArgsFromArgv(argv) {
  const specs = []
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--spec' && argv[i + 1]) {
      specs.push(argv[i + 1])
      i += 1
    } else if (typeof arg === 'string' && arg.startsWith('--spec=')) {
      specs.push(arg.slice('--spec='.length))
    }
  }
  return specs.flatMap((value) =>
    value
      .split(/[,\n]/)
      .map((part) => part.trim())
      .filter(Boolean)
  )
}

/**
 * Cypress 16 strips `--spec` from the config Node process argv. The parent
 * Cypress.app command line still carries it — use that for setup-time mock
 * injection into `expose` (before:run is too late for expose).
 */
export function specArgsFromParentProcess(
  ppid = process.ppid,
  exec = execSync
) {
  if (!Number.isInteger(ppid) || ppid <= 0) return []
  try {
    const command = exec(`ps -p ${ppid} -o command=`, {
      encoding: 'utf8',
    }).trim()
    return specArgsFromArgv(command.split(/\s+/))
  } catch {
    return []
  }
}

function specPatterns(specPattern) {
  if (Array.isArray(specPattern)) return specPattern
  return specPattern ? [specPattern] : []
}

function normalizeSelectedSpec(spec, checkoutRoot) {
  const trimmed = spec.trim().replace(/\\/g, '/')
  const withoutFile = trimmed.startsWith('file:')
    ? trimmed.slice('file:'.length)
    : trimmed
  const absolute = path.isAbsolute(withoutFile)
    ? withoutFile
    : path.resolve(checkoutRoot, withoutFile)
  const relative = path.relative(checkoutRoot, absolute).replace(/\\/g, '/')
  if (!relative.startsWith('..')) {
    return relative.replace(/^\.\//, '')
  }
  return withoutFile.replace(/^\.\//, '')
}

export function selectedCypressSpecs({
  argv,
  specPattern,
  checkoutRoot,
  parentSpecArgs,
}) {
  const fromArgv = specArgsFromArgv(argv)
  const fromParent =
    parentSpecArgs ?? (fromArgv.length === 0 ? specArgsFromParentProcess() : [])
  const raw =
    fromArgv.length > 0
      ? fromArgv
      : fromParent.length > 0
        ? fromParent
        : specPatterns(specPattern)
  return raw.map((spec) => normalizeSelectedSpec(spec, checkoutRoot))
}

function isGlobSpecPattern(spec) {
  return spec.includes('*') || spec.includes('?')
}

export function hasExplicitCypressSpecSelection(
  argv,
  specPattern,
  parentSpecArgs
) {
  if (specArgsFromArgv(argv).length > 0) return true
  const fromParent = parentSpecArgs ?? specArgsFromParentProcess()
  if (fromParent.length > 0) return true
  const patterns = specPatterns(specPattern)
  return (
    patterns.length > 0 &&
    patterns.every((pattern) => !isGlobSpecPattern(pattern))
  )
}

export function specsFromBeforeRun(details, checkoutRoot) {
  const specs = details?.specs ?? []
  return specs.map((spec) =>
    normalizeSelectedSpec(
      spec.relative ?? spec.absolute ?? spec.name ?? String(spec),
      checkoutRoot
    )
  )
}

export function assertSupportedIsolatedCypressSpecs(specs) {
  const approved = specs.map(approvedIsolatedCypressSpec)
  if (approved.length > 0 && approved.every(Boolean)) {
    return {
      requiresPrivateOpenAiMock: approved.some(
        (spec) => spec.requiresPrivateOpenAiMock
      ),
      requiresPrivateWikidataMock: approved.some(
        (spec) => spec.requiresPrivateWikidataMock
      ),
    }
  }
  throw new Error(
    'Isolated Cypress only supports selections from ' +
      `${SUPPORTED_ISOLATED_CYPRESS_SPECS.join(', ')}. ` +
      'Refusing this spec selection so it does not reset, mock, or use shared defaults.'
  )
}
