import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  cloneWithLocalNoteEdit,
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

export function describeNotebookPullAddition(): void {
  describe('notebook pull (eligible accepted note addition)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-accepted-addition-test-'
    )
    const originalNote = '---\ntype: Note\n---\n# Note\n\nOriginal.\n'
    const localBody = '---\ntype: Note\n---\n# Note\n\nLocal body.\n'
    const addedNote = '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n'
    const localFrontmatter =
      '---\ntype: Note\nauthored: local-yaml\n---\n# Note\n\nOriginal.\n'
    const nestedAdded = '---\ntype: Note\n---\n# Added\n\nNested addition.\n'

    test('rebases one local note edit over one accepted root note addition', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        originalNote,
        localBody
      )
      commitPortableFile(
        setup.source,
        'added.md',
        addedNote,
        'accepted addition'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'addition-root')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', setup.directory])

      const localHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(acceptedHead)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        localBody
      )
      expect(fs.readFileSync(join(setup.directory, 'added.md'), 'utf8')).toBe(
        addedNote
      )
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock().mock.calls[0]?.[0]).toContain('/git-bundle')
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased onto the accepted history. Unpublished local commit: ${localHead}. Accepted head: ${acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('receives a nested addition under a local frontmatter edit', async () => {
      const workDir = ctx.getWorkDir()
      const source = buildSourceRepo(workDir)
      commitPortableFile(
        source,
        'Nested/Cell.md',
        '---\ntype: Note\n---\n# Nested\n\nOriginal.\n',
        'add nested folder'
      )
      const directory = cloneAsBoundCheckout(
        workDir,
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      commitPortableFile(
        directory,
        'note.md',
        localFrontmatter,
        'unpublished frontmatter edit'
      )
      commitPortableFile(
        source,
        'Nested/Beta.md',
        nestedAdded,
        'accepted nested addition'
      )
      serveAcceptedBundle(ctx, source, 'addition-nested')

      await run(['notebook', 'pull', directory])

      expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
        localFrontmatter
      )
      expect(
        fs.readFileSync(join(directory, 'Nested', 'Beta.md'), 'utf8')
      ).toBe(nestedAdded)
    })
  })
}
