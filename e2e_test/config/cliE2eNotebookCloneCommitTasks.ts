/**
 * Cypress task handlers that stage and commit a change in a CLI notebook clone
 * checkout. Each task is one Portable-tree change a scenario makes before
 * publishing, and returns the resulting commit id.
 */

import {
  amendCheckout,
  commitCheckout,
  stageEmptyKeep,
  stageExactBytes,
  stageFilledBytes,
  stageNoteChanges,
  stageNoteRemoval,
  stageNoteRename,
} from './cliE2eNotebookCloneGit'

export function createCliE2eNotebookCloneCommitTasks() {
  return {
    commitCliNotebookCheckoutNoteChange({
      checkoutDir,
      files,
    }: {
      checkoutDir: string
      files: { relativePath: string; content: string }[]
    }): string {
      stageNoteChanges(checkoutDir, files)
      return commitCheckout(checkoutDir, 'Change cloned notebook note')
    },
    /**
     * One commit carrying a UTF-8 text root file and a root file whose exact
     * bytes are given as spaced hex.
     */
    commitCliNotebookCheckoutRootFiles({
      checkoutDir,
      textRelativePath,
      content,
      binaryRelativePath,
      bytes,
    }: {
      checkoutDir: string
      textRelativePath: string
      content: string
      binaryRelativePath: string
      bytes: string
    }): string {
      stageNoteChanges(checkoutDir, [
        { relativePath: textRelativePath, content },
      ])
      stageExactBytes(checkoutDir, binaryRelativePath, bytes)
      return commitCheckout(checkoutDir, 'Add cloned notebook root files')
    },
    /** One unpublished commit adding a filled attachment of exact byte length. */
    commitCliNotebookCheckoutFilledAttachment({
      checkoutDir,
      relativePath,
      byteLength,
      fillByte,
    }: {
      checkoutDir: string
      relativePath: string
      byteLength: number
      fillByte: number
    }): string {
      stageFilledBytes(checkoutDir, relativePath, byteLength, fillByte)
      return commitCheckout(checkoutDir, 'Add filled attachment')
    },
    /**
     * Amends the unpublished tip replacing one path with exact spaced-hex bytes.
     */
    amendCliNotebookCheckoutExactBytes({
      checkoutDir,
      relativePath,
      bytes,
    }: {
      checkoutDir: string
      relativePath: string
      bytes: string
    }): string {
      stageExactBytes(checkoutDir, relativePath, bytes)
      return amendCheckout(checkoutDir)
    },
    commitCliNotebookCheckoutNoteRemoval({
      checkoutDir,
      relativePath,
    }: {
      checkoutDir: string
      relativePath: string
    }): string {
      stageNoteRemoval(checkoutDir, relativePath)
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
      stageNoteRename(checkoutDir, fromRelativePath, toRelativePath)
      return commitCheckout(checkoutDir, 'Rename cloned notebook note')
    },
    commitCliNotebookCheckoutNoteRenameAndRemoval({
      checkoutDir,
      fromRelativePath,
      toRelativePath,
      removeRelativePath,
    }: {
      checkoutDir: string
      fromRelativePath: string
      toRelativePath: string
      removeRelativePath: string
    }): string {
      stageNoteRename(checkoutDir, fromRelativePath, toRelativePath)
      stageNoteRemoval(checkoutDir, removeRelativePath)
      return commitCheckout(
        checkoutDir,
        'Rename cloned notebook note and remove a path'
      )
    },
    commitCliNotebookCheckoutNoteRenameAndEmptyKeep({
      checkoutDir,
      fromRelativePath,
      toRelativePath,
      keepRelativePath,
    }: {
      checkoutDir: string
      fromRelativePath: string
      toRelativePath: string
      keepRelativePath: string
    }): string {
      stageNoteRename(checkoutDir, fromRelativePath, toRelativePath)
      stageEmptyKeep(checkoutDir, keepRelativePath)
      return commitCheckout(
        checkoutDir,
        'Rename cloned notebook note and add an empty keep'
      )
    },
    commitCliNotebookCheckoutNoteRenameAndEdit({
      checkoutDir,
      fromRelativePath,
      toRelativePath,
      relativePath,
      content,
    }: {
      checkoutDir: string
      fromRelativePath: string
      toRelativePath: string
      relativePath: string
      content: string
    }): string {
      stageNoteRename(checkoutDir, fromRelativePath, toRelativePath)
      stageNoteChanges(checkoutDir, [{ relativePath, content }])
      return commitCheckout(
        checkoutDir,
        'Rename and change cloned notebook notes'
      )
    },
  }
}
