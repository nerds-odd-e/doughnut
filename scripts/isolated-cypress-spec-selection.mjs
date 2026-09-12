import { execSync } from 'node:child_process'
import path from 'node:path'

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

/**
 * Active CLI feature files without network mocks, assessed 2026-09-12 from
 * `e2e_test/features/cli/`. Each uses `@bundleCliE2eInstall` and (except
 * install-and-run) `@withCliConfig`; spawned CLI processes route to the
 * selected app origin and use `mkdtemp` config/install/clone destinations
 * plus checkout-local bundles. Wholly-ignored CLI files (`@ignore`:
 * `cli_access_token.feature`, `cli_gmail.feature`, `cli_interactive_mode.feature`,
 * `cli_recall.feature`) are never admitted.
 */
const ACTIVE_CLI_SPECS = [
  'e2e_test/features/cli/cli_install_and_run.feature',
  'e2e_test/features/cli/cli_notebook_clone.feature',
  'e2e_test/features/cli/cli_notebook_existing_note_edits.feature',
  'e2e_test/features/cli/cli_notebook_folder_relocation.feature',
]

/** MCP search/graph workflow — tool calls reach the owning worktree's
 * isolated backend and notebook data. */
export const SUPPORTED_ISOLATED_MCP_SPEC =
  'e2e_test/features/mcp/mcp_services.feature'

/** OpenAI completion — currently the only approved spec that requires a
 * runner-owned private OpenAI mock. */
export const SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC =
  'e2e_test/features/ai_generated_content/note_content_completion.feature'

/**
 * Application-only active feature files — no declared network-mock tag and
 * no CLI/MCP/OpenAI-mock/Wikidata-mock/live-provider resource requirement.
 * Assessed 2026-09-12 from the current feature inventory; this is an explicit
 * assessed path list, not a hardcoded count cap. Resource-dependent groups
 * (CLI, OpenAI mock, Wikidata mock, live OpenAI) are admitted by their own
 * slices; wholly-ignored files are never admitted.
 */
const APPLICATION_ONLY_ACTIVE_SPECS = [
  'e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature',
  'e2e_test/features/assimilation/assimilation_page_types.feature',
  'e2e_test/features/assimilation/assimilation_walkthrough.feature',
  'e2e_test/features/assimilation/edit_when_assimilating.feature',
  'e2e_test/features/bazaar/browsing.feature',
  'e2e_test/features/bazaar/sharing.feature',
  'e2e_test/features/book_reading/book_browsing.feature',
  'e2e_test/features/book_reading/reading_record.feature',
  'e2e_test/features/book_reading/reorganize_layout.feature',
  'e2e_test/features/circles/creating_circles.feature',
  'e2e_test/features/circles/notebooks_in_circles.feature',
  'e2e_test/features/folder_organization/folder_organization.feature',
  'e2e_test/features/folder_organization/folder_page_readme.feature',
  'e2e_test/features/learning_session/commissioned_learning_session.feature',
  'e2e_test/features/messages/message_center_with_unread_message_count.feature',
  'e2e_test/features/messages/message_for_note.feature',
  'e2e_test/features/note_creation_and_update/note_creation.feature',
  'e2e_test/features/note_creation_and_update/note_deletion.feature',
  'e2e_test/features/note_creation_and_update/note_edit.feature',
  'e2e_test/features/note_topology/markdown_link.feature',
  'e2e_test/features/note_topology/note_move.feature',
  'e2e_test/features/note_topology/note_property.feature',
  'e2e_test/features/note_topology/note_tree_view.feature',
  'e2e_test/features/note_topology/property_wiki_link.feature',
  'e2e_test/features/note_topology/wiki_link.feature',
  'e2e_test/features/note_topology/wiki_link_insert.feature',
  'e2e_test/features/note_topology/wiki_link_move.feature',
  'e2e_test/features/note_view/note_frontmatter_image.feature',
  'e2e_test/features/note_view/note_recent_update.feature',
  'e2e_test/features/note_view/search_note.feature',
  'e2e_test/features/notebooks/notebook_catalog_navigation.feature',
  'e2e_test/features/notebooks/notebook_creation.feature',
  'e2e_test/features/notebooks/notebook_export.feature',
  'e2e_test/features/notebooks/notebook_group.feature',
  'e2e_test/features/notebooks/notebook_health.feature',
  'e2e_test/features/recall/accidental_match_scheduling.feature',
  'e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature',
  'e2e_test/features/recall/daily_probe.feature',
  'e2e_test/features/recall/overlap_try_again.feature',
  'e2e_test/features/recall/recall_quiz_spelling_question.feature',
  'e2e_test/features/recall/spaced_repetition.feature',
  'e2e_test/features/relationships/add_relationship.feature',
  'e2e_test/features/relationships/relationship_edit_and_remove.feature',
  'e2e_test/features/testability/feature_toggle.feature',
  'e2e_test/features/testability/show_failure_report.feature',
  'e2e_test/features/user_admin/manage_bazaar.feature',
  'e2e_test/features/users/account_control.feature',
  'e2e_test/features/users/new_user.feature',
  'e2e_test/features/users/user_access_token.feature',
  'e2e_test/features/users/user_profile.feature',
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
    }
  }
  throw new Error(
    'Isolated Cypress only supports selections from ' +
      `${SUPPORTED_ISOLATED_CYPRESS_SPECS.join(', ')}. ` +
      'Refusing this spec selection so it does not reset, mock, or use shared defaults.'
  )
}
