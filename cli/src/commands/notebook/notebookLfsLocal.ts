import * as fs from 'node:fs'
import * as path from 'node:path'
import { loadAuthenticatedFetchContext } from '../../backendApi/donutBackendClient.js'
import { runSystemGitOrThrow } from './systemGit.js'

function stripTrailingSlash(apiBaseUrl: string): string {
  return apiBaseUrl.replace(/\/$/, '')
}

function notebookLfsEndpoint(apiBaseUrl: string, notebookId: number): string {
  return `${stripTrailingSlash(apiBaseUrl)}/api/notebooks/${notebookId}/lfs`
}

function placeholderNotebookRemoteUrl(apiBaseUrl: string): string {
  return `${stripTrailingSlash(apiBaseUrl)}/donut-notebook.git`
}

/**
 * Options for worktree-changing Git operations: LFS pointers stay pointers until the
 * authenticated fill-in downloads the current files once.
 */
export function smudgeSkippedGitOptions(): { env: NodeJS.ProcessEnv } {
  return { env: { ...process.env, GIT_LFS_SKIP_SMUDGE: '1' } }
}

/** True when the checkout's `.gitattributes` enables the standard LFS filter. */
export function checkoutUsesLfs(checkoutDir: string): boolean {
  const attributesPath = path.join(checkoutDir, '.gitattributes')
  if (!fs.existsSync(attributesPath)) {
    return false
  }
  return fs.readFileSync(attributesPath, 'utf8').includes('filter=lfs')
}

/** The checkout's local Git config, keyed as Git prints it (section and name lowercased). */
function readLocalGitConfig(checkoutDir: string): Map<string, string> {
  const raw = runSystemGitOrThrow(
    ['-C', checkoutDir, 'config', '--local', '--list', '-z'],
    (detail, status) =>
      `failed to read local Git config${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
  const config = new Map<string, string>()
  for (const entry of raw.split('\0')) {
    if (entry === '') continue
    const newline = entry.indexOf('\n')
    if (newline < 0) config.set(entry, '')
    else config.set(entry.slice(0, newline), entry.slice(newline + 1))
  }
  return config
}

function recordLocalGitConfig(
  checkoutDir: string,
  key: string,
  value: string,
  what: string
): void {
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'config', '--local', key, value],
    (detail, status) =>
      `failed to record local Git LFS ${what}${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
}

/**
 * Throws the "Git LFS is required" failure, naming `purpose` and `nextStep`, when the `git lfs`
 * command is unavailable.
 */
function requireGitLfs(purpose: 'receive' | 'publish', nextStep: string): void {
  runSystemGitOrThrow(
    ['lfs', 'version'],
    (detail, status) =>
      `Git LFS is required to ${purpose} this notebook's attachments${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Install Git LFS, then ${nextStep}.`
  )
}

/**
 * Prepares an LFS checkout for authenticated transfers: records the notebook LFS endpoint and
 * the CLI's current login in local Git config (never authored content), behind a placeholder
 * `origin` when the checkout has none, and installs the local LFS filters. Only what is missing
 * or changed is written. A checkout without the LFS filters first requires Git LFS
 * ({@link requireGitLfs}).
 */
function prepareAuthenticatedLfsCheckout(
  checkoutDir: string,
  notebookId: number,
  purpose: 'receive' | 'publish',
  nextStep: string
): void {
  const config = readLocalGitConfig(checkoutDir)
  const lfsInstalled = config.has('filter.lfs.process')
  if (!lfsInstalled) requireGitLfs(purpose, nextStep)
  const { apiBaseUrl, token } = loadAuthenticatedFetchContext()
  if (!config.has('remote.origin.url')) {
    runSystemGitOrThrow(
      [
        '-C',
        checkoutDir,
        'remote',
        'add',
        'origin',
        placeholderNotebookRemoteUrl(apiBaseUrl),
      ],
      (detail, status) =>
        `failed to add placeholder origin for Git LFS${
          detail ? `: ${detail}` : ` (exit code ${status})`
        }`
    )
  }
  const lfsUrl = notebookLfsEndpoint(apiBaseUrl, notebookId)
  if (config.get('lfs.url') !== lfsUrl) {
    recordLocalGitConfig(checkoutDir, 'lfs.url', lfsUrl, 'endpoint')
  }
  const authorization = `Authorization: Bearer ${token}`
  if (config.get('http.extraheader') !== authorization) {
    recordLocalGitConfig(
      checkoutDir,
      'http.extraHeader',
      authorization,
      'authorization'
    )
  }
  if (!lfsInstalled) {
    runSystemGitOrThrow(
      ['-C', checkoutDir, 'lfs', 'install', '--local'],
      (detail, status) =>
        `failed to configure Git LFS in the checkout${
          detail ? `: ${detail}` : ` (exit code ${status})`
        }`,
      { env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } }
    )
  }
}

/**
 * Runs one Git LFS transfer (`git lfs <lfsArgs>`) against the notebook endpoint with the CLI's
 * current login, after {@link prepareAuthenticatedLfsCheckout}. Transfers never prompt and never
 * smudge. A failure reports a missing Git LFS as required, otherwise `describeFailure`'s message.
 */
export function runAuthenticatedLfsTransfer(
  checkoutDir: string,
  notebookId: number,
  purpose: 'receive' | 'publish',
  nextStep: string,
  lfsArgs: readonly string[],
  describeFailure: (detail: string | undefined, status: number | null) => string
): void {
  prepareAuthenticatedLfsCheckout(checkoutDir, notebookId, purpose, nextStep)
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', ...lfsArgs],
    (detail, status) => {
      requireGitLfs(purpose, nextStep)
      return describeFailure(detail, status)
    },
    { env: { ...smudgeSkippedGitOptions().env, GIT_TERMINAL_PROMPT: '0' } }
  )
}
