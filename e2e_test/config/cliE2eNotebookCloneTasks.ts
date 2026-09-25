/**
 * Cypress task handlers for the CLI `notebook clone` E2E checkout (no PTY): a test-owned
 * temporary destination, and reading back the resulting checkout's file tree.
 * Tasks that stage and commit a change live in `cliE2eNotebookCloneCommitTasks.ts`.
 */

import { spawnSync } from 'node:child_process'
import { existsSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createCliE2eNotebookCloneCommitTasks } from './cliE2eNotebookCloneCommitTasks'
import {
  blobAt,
  checkoutFilledBytesMatch,
  continueRebaseNoninteractively,
  firstParent,
  git,
  listCheckoutFilesRecursively,
  readCheckoutFileHex,
  rebaseMergeExists,
} from './cliE2eNotebookCloneGit'

/** HEAD's first-parent chain, its author and subject, and the porcelain status: two `git` calls. */
export interface CliNotebookCheckoutState {
  head: string
  parent: string
  grandparent: string
  status: string
  author: string
  message: string
}

/** The checked-out branch, its parentless-commit count, and the porcelain status. */
export interface CliNotebookCheckoutBranchState {
  branch: string
  rootCommitCount: string
  status: string
}

export interface CliNotebookCheckoutConflictState {
  rebaseMerge: boolean
  unmerged: string
}

export function createCliE2eNotebookCloneTasks() {
  return {
    ...createCliE2eNotebookCloneCommitTasks(),
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
      const [head, parent, grandparent] = git(
        checkoutDir,
        'log',
        '-3',
        '--first-parent',
        '--format=%H%x1f%an <%ae>%x1f%s'
      )
        .split('\n')
        .map((line) => line.split('\x1f'))
      return {
        head: head?.[0] ?? '',
        parent: parent?.[0] ?? '',
        grandparent: grandparent?.[0] ?? '',
        status: git(checkoutDir, 'status', '--porcelain'),
        author: head?.[1] ?? '',
        message: head?.[2] ?? '',
      }
    },
    readCliNotebookCheckoutBranchState(
      checkoutDir: string
    ): CliNotebookCheckoutBranchState {
      return {
        branch: git(checkoutDir, 'rev-parse', '--abbrev-ref', 'HEAD'),
        rootCommitCount: git(
          checkoutDir,
          'rev-list',
          '--max-parents=0',
          '--count',
          'HEAD'
        ),
        status: git(checkoutDir, 'status', '--porcelain'),
      }
    },
    /** Blob id of `relativePath` in commit `treeish`, or '' when absent there. */
    readCliNotebookCheckoutBlob({
      checkoutDir,
      treeish,
      relativePath,
    }: {
      checkoutDir: string
      treeish: string
      relativePath: string
    }): string {
      return blobAt(checkoutDir, treeish, relativePath)
    },
    readCliNotebookCheckoutParentFile({
      checkoutDir,
      relativePath,
    }: {
      checkoutDir: string
      relativePath: string
    }): string {
      const parent = firstParent(checkoutDir)
      if (!parent) {
        throw new Error(`HEAD in ${checkoutDir} has no parent`)
      }
      return git(checkoutDir, 'show', `${parent}:${relativePath}`)
    },
    /** Relative file paths of the checkout, excluding reserved Git entries, for canonical-tree assertions. */
    listNotebookCheckoutEntries(checkoutDir: string): string[] {
      return listCheckoutFilesRecursively(checkoutDir, checkoutDir).sort()
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
      return {
        rebaseMerge: rebaseMergeExists(checkoutDir),
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
    /** On-disk bytes of a checked-out file as lowercase hex. */
    readCliNotebookCheckoutFileHex({
      checkoutDir,
      relativePath,
    }: {
      checkoutDir: string
      relativePath: string
    }): string {
      return readCheckoutFileHex(checkoutDir, relativePath)
    },
    /**
     * Whether a checked-out path is still exactly `byteLength` copies of
     * `fillByte` (retained oversized proposal after a rejected publish).
     */
    assertCliNotebookCheckoutFilledBytes({
      checkoutDir,
      relativePath,
      byteLength,
      fillByte,
    }: {
      checkoutDir: string
      relativePath: string
      byteLength: number
      fillByte: number
    }): null {
      if (
        !checkoutFilledBytesMatch(
          checkoutDir,
          relativePath,
          byteLength,
          fillByte
        )
      ) {
        throw new Error(
          `${relativePath} should still be ${byteLength} bytes filled with 0x${fillByte
            .toString(16)
            .padStart(2, '0')}`
        )
      }
      return null
    },
  }
}
