/**
 * Cypress tasks for publishing LFS attachments from a CLI notebook checkout
 * and observing publish/fresh-clone size proof.
 */

import { randomBytes } from 'node:crypto'
import {
  existsSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import {
  commitWithE2eAuthor,
  lfsObjectPath,
  runOrThrow,
  sha256Hex,
} from './cliE2eNotebookLfsHelpers'

type LfsCommitResult = {
  head: string
  oid: string
  size: number
  gitBlobSize: number
  pointerText: string
}

function commitLfsAttachmentBytes(
  checkoutDir: string,
  relativePath: string,
  bytes: Buffer,
  message: string
): LfsCommitResult {
  const oid = sha256Hex(bytes)
  const filePath = join(checkoutDir, relativePath)
  mkdirSync(dirname(filePath), { recursive: true })
  writeFileSync(filePath, bytes)
  runOrThrow('git', ['add', '--', relativePath], checkoutDir)
  const head = commitWithE2eAuthor(checkoutDir, message)
  const pointerText = runOrThrow(
    'git',
    ['cat-file', 'blob', `${head}:${relativePath}`],
    checkoutDir
  )
  const gitBlobSize = Number(
    runOrThrow(
      'git',
      ['cat-file', '-s', `${head}:${relativePath}`],
      checkoutDir
    ).trim()
  )
  if (!pointerText.includes(`oid sha256:${oid}`)) {
    throw new Error(
      `expected LFS pointer for ${oid}, got blob (${gitBlobSize} bytes):\n${pointerText}`
    )
  }
  return { head, oid, size: bytes.length, gitBlobSize, pointerText }
}

export function createCliE2eNotebookLfsPublishTasks() {
  return {
    /**
     * Commits a filled attachment through the checkout's Git LFS filter.
     * Returns the commit id, payload digest, and Git blob size (pointer bytes).
     */
    commitCliNotebookCheckoutLfsFilledAttachment({
      checkoutDir,
      relativePath,
      byteLength,
      fillByte,
      message,
      noteEdit,
    }: {
      checkoutDir: string
      relativePath: string
      byteLength: number
      fillByte: number
      message?: string
      noteEdit?: { relativePath: string; content: string }
    }): LfsCommitResult {
      if (noteEdit) {
        writeFileSync(
          join(checkoutDir, noteEdit.relativePath),
          noteEdit.content
        )
        runOrThrow('git', ['add', '--', noteEdit.relativePath], checkoutDir)
      }
      return commitLfsAttachmentBytes(
        checkoutDir,
        relativePath,
        Buffer.alloc(byteLength, fillByte),
        message ?? `LFS attachment ${relativePath}`
      )
    },
    /**
     * Commits an incompressible random attachment through the LFS filter.
     */
    commitCliNotebookCheckoutLfsRandomAttachment({
      checkoutDir,
      relativePath,
      byteLength,
      message,
    }: {
      checkoutDir: string
      relativePath: string
      byteLength: number
      message?: string
    }): LfsCommitResult {
      return commitLfsAttachmentBytes(
        checkoutDir,
        relativePath,
        randomBytes(byteLength),
        message ?? `LFS random attachment ${relativePath}`
      )
    },
    measureCliNotebookCheckoutBundleBytes(checkoutDir: string): number {
      const bundleFile = join(checkoutDir, '..', `measure-${Date.now()}.bundle`)
      try {
        runOrThrow('git', ['bundle', 'create', bundleFile, 'main'], checkoutDir)
        return readFileSync(bundleFile).length
      } finally {
        rmSync(bundleFile, { force: true })
      }
    },
    lfsObjectFileSize({
      checkoutDir,
      oid,
    }: {
      checkoutDir: string
      oid: string
    }): number | null {
      const path = lfsObjectPath(checkoutDir, oid)
      if (!existsSync(path)) return null
      return readFileSync(path).length
    },
    gitMergeBaseIsAncestor({
      checkoutDir,
      ancestor,
      descendant,
    }: {
      checkoutDir: string
      ancestor: string
      descendant: string
    }): number {
      const result = spawnSync(
        'git',
        [
          '-C',
          checkoutDir,
          'merge-base',
          '--is-ancestor',
          ancestor,
          descendant,
        ],
        { encoding: 'utf8' }
      )
      return result.status ?? 1
    },
    readCheckoutFileFilledSummary({
      checkoutDir,
      relativePath,
    }: {
      checkoutDir: string
      relativePath: string
    }): { length: number; firstByte: number } {
      const bytes = readFileSync(join(checkoutDir, relativePath))
      return { length: bytes.length, firstByte: bytes[0] ?? -1 }
    },
  }
}
