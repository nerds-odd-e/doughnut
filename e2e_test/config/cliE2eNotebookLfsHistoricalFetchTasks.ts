/**
 * Cypress tasks for recovering a published LFS version via standard
 * `git lfs fetch origin <ref>` after clearing the local object cache.
 */

import { existsSync, readFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import {
  clearLfsObjectCache,
  commitWithE2eAuthor,
  lfsObjectPath,
  runOrThrow,
  sha256Hex,
} from './cliE2eNotebookLfsHelpers'

export function createCliE2eNotebookLfsHistoricalFetchTasks() {
  return {
    /**
     * Removes `.git/lfs/objects` so a later `git lfs fetch <remote> <ref>` must
     * re-download from the notebook LFS endpoint (Git LFS 3.7.1 has no
     * `fetch --object-id`).
     */
    clearCliNotebookCheckoutLfsObjectCache(checkoutDir: string): null {
      clearLfsObjectCache(checkoutDir)
      return null
    },
    /**
     * Standard historical fetch: `git lfs fetch origin <ref>` using the
     * checkout's configured `lfs.url` and `http.extraHeader`.
     */
    fetchCliNotebookCheckoutLfsRef({
      checkoutDir,
      ref,
    }: {
      checkoutDir: string
      ref: string
    }): { status: number | null; output: string } {
      const result = spawnSync('git', ['lfs', 'fetch', 'origin', ref], {
        cwd: checkoutDir,
        encoding: 'utf8',
      })
      return {
        status: result.status,
        output: `${result.stdout ?? ''}\n${result.stderr ?? ''}`,
      }
    },
    readLfsObjectFilledSummary({
      checkoutDir,
      oid,
    }: {
      checkoutDir: string
      oid: string
    }): { length: number; firstByte: number; digest: string } {
      const path = lfsObjectPath(checkoutDir, oid)
      if (!existsSync(path)) {
        throw new Error(
          `LFS object ${oid} is not in the local cache at ${path}`
        )
      }
      const bytes = readFileSync(path)
      return {
        length: bytes.length,
        firstByte: bytes[0] ?? -1,
        digest: sha256Hex(bytes),
      }
    },
    /**
     * Removes a tracked attachment via `git rm` and commits; publish separately.
     */
    commitCliNotebookCheckoutAttachmentRemoval({
      checkoutDir,
      relativePath,
      message,
    }: {
      checkoutDir: string
      relativePath: string
      message?: string
    }): { head: string } {
      runOrThrow('git', ['rm', '--', relativePath], checkoutDir)
      return {
        head: commitWithE2eAuthor(
          checkoutDir,
          message ?? `remove ${relativePath}`
        ),
      }
    },
  }
}
