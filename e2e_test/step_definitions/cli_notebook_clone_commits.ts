/**
 * Cloned-checkout commit steps: keep glue as one-line calls to
 * `e2e_test/start/pageObjects/cli`. Clone, publish, and pull stay in
 * `cli_notebook_clone.ts`.
 */
import { When, type DataTable } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

When(
  'I commit the following edit to {string} in the cloned checkout:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().commitEdit(relativePath, content)
)

When(
  'I commit the following edit to {string} in the second cloned checkout:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().commitReceiverEdit(relativePath, content)
)

When(
  'I add and commit the following note at {string} in the cloned checkout:',
  (relativePath: string, content: string) =>
    cli.notebookCloneCheckout().commitAddition(relativePath, content)
)

function relatedNoteChanges(data: DataTable) {
  return data
    .hashes()
    .map(({ path, content }) => ({ relativePath: path, content }))
}

When(
  'I author and commit the following initial tree in the cloned checkout:',
  (data: DataTable) =>
    cli
      .notebookCloneCheckout()
      .commitRelatedNoteChanges(relatedNoteChanges(data))
)

When(
  'I commit the following related additions and edit together in the cloned checkout:',
  (data: DataTable) =>
    cli
      .notebookCloneCheckout()
      .commitRelatedNoteChanges(relatedNoteChanges(data))
)

When(
  'I commit the following related edits together in the cloned checkout:',
  (data: DataTable) =>
    cli
      .notebookCloneCheckout()
      .commitRelatedNoteChanges(relatedNoteChanges(data))
)

When(
  'I commit the following document changes together in the cloned checkout:',
  (data: DataTable) =>
    cli
      .notebookCloneCheckout()
      .commitRelatedNoteChanges(relatedNoteChanges(data))
)

When(
  'I commit the file {string} and the file {string} holding the bytes {string} together in the cloned checkout:',
  (
    textRelativePath: string,
    binaryRelativePath: string,
    bytes: string,
    content: string
  ) =>
    cli
      .notebookCloneCheckout()
      .commitTextAndBinaryFiles(
        textRelativePath,
        content,
        binaryRelativePath,
        bytes
      )
)

When(
  'I commit the attachment {string} filled with {int} bytes of {string} in the cloned checkout',
  (relativePath: string, byteLength: number, fillByteHex: string) =>
    cli
      .notebookCloneCheckout()
      .commitFilledAttachment(relativePath, byteLength, fillByteHex)
)

When(
  'I amend the unpublished commit replacing {string} with the bytes {string} in the cloned checkout',
  (relativePath: string, bytes: string) =>
    cli.notebookCloneCheckout().amendExactBytes(relativePath, bytes)
)

When(
  'I commit a removal of {string} in the cloned checkout',
  (relativePath: string) =>
    cli.notebookCloneCheckout().commitRemoval(relativePath)
)

When(
  'I commit a rename of {string} to {string} in the cloned checkout',
  (fromRelativePath: string, toRelativePath: string) =>
    cli.notebookCloneCheckout().commitRename(fromRelativePath, toRelativePath)
)

When(
  'I commit a rename of {string} to {string} and a removal of {string} together in the cloned checkout',
  (
    fromRelativePath: string,
    toRelativePath: string,
    removeRelativePath: string
  ) =>
    cli
      .notebookCloneCheckout()
      .commitRenameAndRemoval(
        fromRelativePath,
        toRelativePath,
        removeRelativePath
      )
)

When(
  'I commit a rename of {string} to {string} and an empty keep at {string} together in the cloned checkout',
  (
    fromRelativePath: string,
    toRelativePath: string,
    keepRelativePath: string
  ) =>
    cli
      .notebookCloneCheckout()
      .commitRenameAndEmptyKeep(
        fromRelativePath,
        toRelativePath,
        keepRelativePath
      )
)

When(
  'I commit a rename of {string} to {string} and the following unrelated edit to {string} together in the cloned checkout:',
  (
    fromRelativePath: string,
    toRelativePath: string,
    relativePath: string,
    content: string
  ) =>
    cli
      .notebookCloneCheckout()
      .commitRenameAndEdit(
        fromRelativePath,
        toRelativePath,
        relativePath,
        content
      )
)

When(
  'I commit a rename of {string} to {string} and the following edit to the renamed note together in the cloned checkout:',
  (fromRelativePath: string, toRelativePath: string, content: string) =>
    cli
      .notebookCloneCheckout()
      .commitRenameAndEdit(
        fromRelativePath,
        toRelativePath,
        toRelativePath,
        content
      )
)
