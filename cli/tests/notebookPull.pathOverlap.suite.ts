import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

const NOTE_PATH_OVERLAP =
  'Local main cannot receive the accepted history because accepted history also edited "note.md". Same-note reconciliation is not supported yet.'

export function describeNotebookPullPathOverlap(): void {
  describe('notebook pull (same-path overlap)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-path-overlap-test-'
    )

    test('names the overlapping path for disjoint same-note edits and leaves the checkout unchanged', async () => {
      const { directory, source } = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        '---\ntype: Note\n---\n# Note\n\nLocal paragraph.\nShared line.\n'
      )
      fs.writeFileSync(
        join(source, 'note.md'),
        '---\ntype: Note\n---\n# Note\n\nShared line.\nRemote paragraph.\n'
      )
      runGit(['add', 'note.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted disjoint paragraph'], source)
      serveAcceptedBundle(ctx, source, 'disjoint-paragraphs')
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${NOTE_PATH_OVERLAP}`
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('names the overlapping path when accepted history edited then restored it', async () => {
      const { directory, source } = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        '---\ntype: Note\n---\n# Note\n\nLocal body.\n'
      )
      const original = fs.readFileSync(join(source, 'note.md'), 'utf8')
      fs.writeFileSync(
        join(source, 'note.md'),
        '---\ntype: Note\n---\n# Note\n\nRemote body.\n'
      )
      runGit(['add', 'note.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted remote edit'], source)
      fs.writeFileSync(join(source, 'note.md'), original)
      runGit(['add', 'note.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted remote revert'], source)
      serveAcceptedBundle(ctx, source, 'edit-then-revert')

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${NOTE_PATH_OVERLAP}`
      )
    })
  })
}

function cloneWithLocalNoteEdit(
  workDir: string,
  localBytes: string
): { directory: string; source: string } {
  const source = buildSourceRepo(workDir)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  fs.writeFileSync(join(directory, 'note.md'), localBytes)
  runGit(['add', 'note.md'], directory)
  runGit(['commit', '--quiet', '-m', 'unpublished note edit'], directory)
  return { directory, source }
}
