/**
 * Cypress task handlers for the CLI `notebook clone` E2E checkout (no PTY): a test-owned
 * temporary destination, and reading back the resulting checkout's file tree.
 */

import { execFileSync, spawnSync } from 'node:child_process'
import { existsSync, mkdtempSync, readdirSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, relative } from 'node:path'

export interface CliNotebookCheckoutState {
  head: string
  parent: string
  branch: string
  rootCommitCount: string
  status: string
  author: string
  message: string
  blobs: Record<string, string>
  parentBlobs: Record<string, string>
}

export interface CliNotebookCheckoutConflictState {
  rebaseMerge: boolean
  unmerged: string
}

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

function listFilesRecursively(dir: string, base: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(dir, entry.name)
    if (entry.name === '.git') return []
    if (entry.isDirectory()) return listFilesRecursively(entryPath, base)
    return [relative(base, entryPath)]
  })
}

function git(checkoutDir: string, ...args: string[]): string {
  return execFileSync('git', ['-C', checkoutDir, ...args], {
    encoding: 'utf8',
  }).trim()
}

function commitCheckout(checkoutDir: string, message: string): string {
  git(checkoutDir, ...E2E_GIT_IDENTITY_ARGS, 'commit', '-m', message)
  return git(checkoutDir, 'rev-parse', 'HEAD')
}

function continueRebaseNoninteractively(checkoutDir: string): void {
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

function firstParent(checkoutDir: string): string {
  const parts = git(
    checkoutDir,
    'rev-list',
    '--parents',
    '-n',
    '1',
    'HEAD'
  ).split(' ')
  return parts[1] ?? ''
}

function blobsAt(checkoutDir: string, treeish: string): Record<string, string> {
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

export function createCliE2eNotebookCloneTasks() {
  return {
    /** A destination path that does not yet exist, inside a fresh test-owned temp dir. */
    createCliNotebookCloneDestination(): string {
      const parent = mkdtempSync(join(tmpdir(), 'cypress-cli-clone-'))
      return join(parent, 'checkout')
    },
    cliNotebookCloneDestinationExists(destination: string): boolean {
      return existsSync(destination)
    },
    readCliNotebookCheckoutState(
      checkoutDir: string
    ): CliNotebookCheckoutState {
      const parent = firstParent(checkoutDir)
      return {
        head: git(checkoutDir, 'rev-parse', 'HEAD'),
        parent,
        branch: git(checkoutDir, 'rev-parse', '--abbrev-ref', 'HEAD'),
        rootCommitCount: git(
          checkoutDir,
          'rev-list',
          '--max-parents=0',
          '--count',
          'HEAD'
        ),
        status: git(checkoutDir, 'status', '--porcelain'),
        author: git(checkoutDir, 'log', '-1', '--format=%an <%ae>'),
        message: git(checkoutDir, 'log', '-1', '--format=%s'),
        blobs: blobsAt(checkoutDir, 'HEAD'),
        parentBlobs: parent ? blobsAt(checkoutDir, parent) : {},
      }
    },
    /** Relative file paths of the checkout, excluding `.git`, for canonical-tree assertions. */
    listNotebookCheckoutEntries(checkoutDir: string): string[] {
      return listFilesRecursively(checkoutDir, checkoutDir).sort()
    },
    /**
     * Whether `ancestor` is an ancestor of `HEAD` (`git merge-base --is-ancestor`).
     * Status 1 is “not an ancestor”; any other non-zero is a Git failure.
     */
    cliNotebookCheckoutIsAncestorOfHead({
      checkoutDir,
      ancestor,
    }: {
      checkoutDir: string
      ancestor: string
    }): boolean {
      const result = spawnSync(
        'git',
        ['-C', checkoutDir, 'merge-base', '--is-ancestor', ancestor, 'HEAD'],
        { encoding: 'utf8' }
      )
      if (result.status === 0) return true
      if (result.status === 1) return false
      throw new Error(
        `git merge-base --is-ancestor failed:\n${result.stdout}\n${result.stderr}`
      )
    },
    readCliNotebookCheckoutConflictState(
      checkoutDir: string
    ): CliNotebookCheckoutConflictState {
      const rebaseMergePath = git(
        checkoutDir,
        'rev-parse',
        '--git-path',
        'rebase-merge'
      )
      return {
        rebaseMerge: existsSync(join(checkoutDir, rebaseMergePath)),
        unmerged: git(checkoutDir, 'ls-files', '-u'),
      }
    },
    continueCliNotebookCheckoutRebaseWithChosenBytes({
      checkoutDir,
      relativePath,
      content,
    }: {
      checkoutDir: string
      relativePath: string
      content: string
    }): null {
      writeFileSync(join(checkoutDir, relativePath), `${content}\n`)
      git(checkoutDir, 'add', '--', relativePath)
      continueRebaseNoninteractively(checkoutDir)
      return null
    },
    commitCliNotebookCheckoutNoteChange({
      checkoutDir,
      files,
    }: {
      checkoutDir: string
      files: { relativePath: string; content: string }[]
    }): string {
      for (const { relativePath, content } of files) {
        writeFileSync(join(checkoutDir, relativePath), `${content}\n`)
      }
      git(
        checkoutDir,
        'add',
        '--',
        ...files.map(({ relativePath }) => relativePath)
      )
      return commitCheckout(checkoutDir, 'Change cloned notebook note')
    },
    commitCliNotebookCheckoutNoteRemoval({
      checkoutDir,
      relativePath,
    }: {
      checkoutDir: string
      relativePath: string
    }): string {
      git(checkoutDir, 'rm', '--', relativePath)
      return commitCheckout(checkoutDir, 'Remove cloned notebook note')
    },
    commitCliNotebookCheckoutNoteRename({
      checkoutDir,
      fromRelativePath,
      toRelativePath,
    }: {
      checkoutDir: string
      fromRelativePath: string
      toRelativePath: string
    }): string {
      git(checkoutDir, 'mv', fromRelativePath, toRelativePath)
      return commitCheckout(checkoutDir, 'Rename cloned notebook note')
    },
  }
}
