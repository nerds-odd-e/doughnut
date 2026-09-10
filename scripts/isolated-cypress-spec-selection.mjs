import { execSync } from 'node:child_process'
import path from 'node:path'

/** No-mock note editing — remains the default allowlisted focused spec. */
export const SUPPORTED_ISOLATED_CYPRESS_SPEC =
  'e2e_test/features/note_creation_and_update/worktree_note_editing.feature'

/** Web-created-note CLI workflow — clones, creates, pulls, and publishes
 * against the owning worktree's own isolated backend and notebook data. */
export const SUPPORTED_ISOLATED_CLI_SPEC =
  'e2e_test/features/cli/cli_notebook_web_created_note.feature'

/** MCP search/graph workflow — tool calls reach the owning worktree's
 * isolated backend and notebook data. */
export const SUPPORTED_ISOLATED_MCP_SPEC =
  'e2e_test/features/mcp/mcp_services.feature'

/** OpenAI completion — currently the only approved spec that requires a
 * runner-owned private OpenAI mock. */
export const SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC =
  'e2e_test/features/ai_generated_content/note_content_completion.feature'

const APPROVED_ISOLATED_CYPRESS_SPECS = [
  { spec: SUPPORTED_ISOLATED_CYPRESS_SPEC },
  { spec: SUPPORTED_ISOLATED_CLI_SPEC },
  { spec: SUPPORTED_ISOLATED_MCP_SPEC },
  {
    spec: SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
    requiresPrivateOpenAiMock: true,
  },
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
      .split(',')
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
  if (specs.length === 1 && approvedIsolatedCypressSpec(specs[0])) {
    return
  }
  throw new Error(
    'Isolated Cypress only supports one of ' +
      `${SUPPORTED_ISOLATED_CYPRESS_SPECS.join(' or ')}. ` +
      'Refusing this spec selection so it does not reset, mock, or use shared defaults.'
  )
}
