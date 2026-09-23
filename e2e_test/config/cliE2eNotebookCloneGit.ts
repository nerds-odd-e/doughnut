/**
 * Test-owned Git plumbing for CLI notebook clone checkouts: identity, staging,
 * commit, rebase continue, and tree and on-disk reads. Cypress task names stay in
 * `cliE2eNotebookCloneTasks.ts`.
 */

import { execFileSync, spawnSync } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  writeFileSync,
} from 'node:fs'
import { dirname, join, relative } from 'node:path'
import { hexFromSpacedHex } from './spacedHexBytes'

const E2E_GIT_IDENTITY_ARGS = [
  '-c',
  'user.name=Donut E2E',
  '-c',
  'user.email=donut-e2e@example.com',
] as const

const TEST_OWNED_REBASE_EDITOR_ENV = {
  GIT_EDITOR: 'true',
  GIT_SEQUENCE_EDITOR: 'true',
} as const

export function listCheckoutFilesRecursively(
  dir: string,
  base: string
): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(dir, entry.name)
    if (entry.name === '.git') return []
    if (entry.isDirectory())
      return listCheckoutFilesRecursively(entryPath, base)
    return [relative(base, entryPath)]
  })
}

export function git(checkoutDir: string, ...args: string[]): string {
  return execFileSync('git', ['-C', checkoutDir, ...args], {
    encoding: 'utf8',
  }).trim()
}

export function commitCheckout(checkoutDir: string, message: string): string {
  git(checkoutDir, ...E2E_GIT_IDENTITY_ARGS, 'commit', '-m', message)
  return git(checkoutDir, 'rev-parse', 'HEAD')
}

export function stageNoteChanges(
  checkoutDir: string,
  files: { relativePath: string; content: string }[]
): void {
  for (const { relativePath, content } of files) {
    const filePath = join(checkoutDir, relativePath)
    mkdirSync(dirname(filePath), { recursive: true })
    writeFileSync(filePath, `${content}\n`)
  }
  git(
    checkoutDir,
    'add',
    '--',
    ...files.map(({ relativePath }) => relativePath)
  )
}

/** Writes raw bytes at a relative path and stages them. */
function stageFileBytes(
  checkoutDir: string,
  relativePath: string,
  bytes: Buffer
): void {
  const filePath = join(checkoutDir, relativePath)
  mkdirSync(dirname(filePath), { recursive: true })
  writeFileSync(filePath, bytes)
  git(checkoutDir, 'add', '--', relativePath)
}

/** Stages exactly the bytes a spaced-hex fixture names. */
export function stageExactBytes(
  checkoutDir: string,
  relativePath: string,
  spacedHex: string
): void {
  stageFileBytes(
    checkoutDir,
    relativePath,
    Buffer.from(hexFromSpacedHex(spacedHex), 'hex')
  )
}

/** Stages a deterministic filled attachment of exact length (one repeated byte). */
export function stageFilledBytes(
  checkoutDir: string,
  relativePath: string,
  byteLength: number,
  fillByte: number
): void {
  stageFileBytes(checkoutDir, relativePath, Buffer.alloc(byteLength, fillByte))
}

/** True when the on-disk file is exactly `byteLength` copies of `fillByte`. */
export function checkoutFilledBytesMatch(
  checkoutDir: string,
  relativePath: string,
  byteLength: number,
  fillByte: number
): boolean {
  const actual = readFileSync(join(checkoutDir, relativePath))
  if (actual.length !== byteLength) {
    return false
  }
  return actual.equals(Buffer.alloc(byteLength, fillByte))
}

/** Amends HEAD in place (unpublished tip) after staging; returns the new commit id. */
export function amendCheckout(checkoutDir: string): string {
  git(checkoutDir, ...E2E_GIT_IDENTITY_ARGS, 'commit', '--amend', '--no-edit')
  return git(checkoutDir, 'rev-parse', 'HEAD')
}

/** The checked-out file's bytes as lowercase hex, never decoded as text. */
export function readCheckoutFileHex(
  checkoutDir: string,
  relativePath: string
): string {
  return readFileSync(join(checkoutDir, relativePath)).toString('hex')
}

export function stageNoteRemoval(
  checkoutDir: string,
  relativePath: string
): void {
  git(checkoutDir, 'rm', '--', relativePath)
}

export function stageNoteRename(
  checkoutDir: string,
  fromRelativePath: string,
  toRelativePath: string
): void {
  git(checkoutDir, 'mv', fromRelativePath, toRelativePath)
}

export function stageEmptyKeep(
  checkoutDir: string,
  relativePath: string
): void {
  stageFileBytes(checkoutDir, relativePath, Buffer.alloc(0))
}

export function continueRebaseNoninteractively(checkoutDir: string): void {
  const result = spawnSync(
    'git',
    ['-C', checkoutDir, ...E2E_GIT_IDENTITY_ARGS, 'rebase', '--continue'],
    {
      encoding: 'utf8',
      env: { ...process.env, ...TEST_OWNED_REBASE_EDITOR_ENV },
    }
  )
  if (result.status !== 0) {
    throw new Error(
      `git rebase --continue failed:\n${result.stdout}\n${result.stderr}`
    )
  }
}

export function firstParent(checkoutDir: string, treeish = 'HEAD'): string {
  const parts = git(
    checkoutDir,
    'rev-list',
    '--parents',
    '-n',
    '1',
    treeish
  ).split(' ')
  return parts[1] ?? ''
}

export function blobsAt(
  checkoutDir: string,
  treeish: string
): Record<string, string> {
  const output = git(checkoutDir, 'ls-tree', '-r', treeish)
  if (!output) return {}
  return Object.fromEntries(
    output.split('\n').map((line) => {
      const tab = line.indexOf('\t')
      if (tab < 0) {
        throw new Error(`unexpected git ls-tree line: ${line}`)
      }
      const hash = line.slice(0, tab).split(' ')[2] ?? ''
      return [line.slice(tab + 1), hash]
    })
  )
}

export function rebaseMergeExists(checkoutDir: string): boolean {
  const rebaseMergePath = git(
    checkoutDir,
    'rev-parse',
    '--git-path',
    'rebase-merge'
  )
  return existsSync(join(checkoutDir, rebaseMergePath))
}
