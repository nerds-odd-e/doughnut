/**
 * Cypress tasks for recovering a published LFS version via the documented
 * `git lfs fetch "$(git config lfs.url)" <ref>` after clearing the local
 * object cache.
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
     * Removes `.git/lfs/objects` so a later `git lfs fetch <endpoint> <ref>` must
     * re-download from the notebook LFS endpoint (Git LFS 3.7.1 has no
     * `fetch --object-id`).
     */
    clearCliNotebookCheckoutLfsObjectCache(checkoutDir: string): null {
      clearLfsObjectCache(checkoutDir)
      return null
    },
    listCliNotebookCheckoutRemotes(checkoutDir: string): string[] {
      return runOrThrow('git', ['remote'], checkoutDir)
        .split('\n')
        .filter((name) => name.length > 0)
    },
    /**
     * Documented historical fetch: `git lfs fetch <lfs.url> <ref>`, naming the
     * checkout's configured endpoint (no remote needed) and authorized by its
     * `http.extraHeader`.
     */
    fetchCliNotebookCheckoutLfsRef({
      checkoutDir,
      ref,
    }: {
      checkoutDir: string
      ref: string
    }): { status: number | null; output: string } {
      const lfsUrl = runOrThrow(
        'git',
        ['config', 'lfs.url'],
        checkoutDir
      ).trim()
      const result = spawnSync('git', ['lfs', 'fetch', lfsUrl, ref], {
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
