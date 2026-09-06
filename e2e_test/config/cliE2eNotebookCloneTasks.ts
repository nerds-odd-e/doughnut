/**
 * Cypress task handlers for the CLI `notebook clone` E2E checkout (no PTY): a test-owned
 * temporary destination, and reading back the resulting checkout's file tree.
 */

import { execFileSync } from 'node:child_process'
import { existsSync, mkdtempSync, readdirSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, relative } from 'node:path'

export interface CliNotebookCheckoutState {
  head: string
  branch: string
  rootCommitCount: string
  status: string
}

function listFilesRecursively(dir: string, base: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = join(dir, entry.name)
    if (entry.name === '.git') return []
    if (entry.isDirectory()) return listFilesRecursively(entryPath, base)
    return [relative(base, entryPath)]
  })
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
      const git = (...args: string[]) =>
        execFileSync('git', ['-C', checkoutDir, ...args], {
          encoding: 'utf8',
        }).trim()
      return {
        head: git('rev-parse', 'HEAD'),
        branch: git('rev-parse', '--abbrev-ref', 'HEAD'),
        rootCommitCount: git('rev-list', '--max-parents=0', '--count', 'HEAD'),
        status: git('status', '--porcelain'),
      }
    },
    /** Relative file paths of the checkout, excluding `.git`, for canonical-tree assertions. */
    listNotebookCheckoutEntries(checkoutDir: string): string[] {
      return listFilesRecursively(checkoutDir, checkoutDir).sort()
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
      execFileSync('git', [
        '-C',
        checkoutDir,
        'add',
        '--',
        ...files.map(({ relativePath }) => relativePath),
      ])
      execFileSync(
        'git',
        [
          '-C',
          checkoutDir,
          '-c',
          'user.name=Donut E2E',
          '-c',
          'user.email=donut-e2e@example.com',
          'commit',
          '-m',
          'Change cloned notebook note',
        ],
        { encoding: 'utf8' }
      )
      return execFileSync('git', ['-C', checkoutDir, 'rev-parse', 'HEAD'], {
        encoding: 'utf8',
      }).trim()
    },
  }
}
