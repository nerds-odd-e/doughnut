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
import { installNotebookPullAcceptedHistoryTest } from './notebookPull.testHelpers.js'
import { serveAcceptedBundle } from './notebookPull.historySafety.testHelpers.js'
import {
  LOCAL_NOTE,
  NESTED_NOTE,
  OTHER_NOTE,
  prepareEligibleDivergence,
} from './notebookPull.rebase.testHelpers.js'

export function describeNotebookPullRebase(): void {
  describe('notebook pull (eligible other-note rebase)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-rebase-test-'
    )

    test('rebases one local note edit over one accepted other-note commit', async () => {
      const setup = prepareEligibleDivergence(ctx.getWorkDir(), {
        remoteEdits: 1,
      })
      serveAcceptedBundle(ctx, setup.source, 'rebase-one')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()
      const originalAuthor = runGit(
        ['log', '-1', '--format=%an <%ae>', setup.localTip],
        setup.directory
      )
      const originalMessage = runGit(
        ['log', '-1', '--format=%s', setup.localTip],
        setup.directory
      )

      await run(['notebook', 'pull', setup.directory])

      const localHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', setup.acceptedHead], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
      expect(fs.readFileSync(join(setup.directory, 'other.md'), 'utf8')).toBe(
        OTHER_NOTE
      )
      expect(runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)).toBe(
        originalAuthor
      )
      expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
        originalMessage
      )
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(
        runGit(
          ['config', '--local', '--get', 'donut.notebook-id'],
          setup.directory
        )
      ).toBe('42')
      expect(
        runGit(['for-each-ref', '--format=%(refname)'], setup.directory)
      ).toBe('refs/heads/main')
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock().mock.calls[0]?.[0]).toContain('/git-bundle')
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased the unpublished local commit onto the accepted history. Local head: ${localHead}. Accepted head: ${setup.acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('keeps several accepted other-note commits unchanged under one rebased local child', async () => {
      const setup = prepareEligibleDivergence(ctx.getWorkDir(), {
        remoteEdits: 3,
      })
      serveAcceptedBundle(ctx, setup.source, 'rebase-several')
      const olderAccepted = runGit(['rev-parse', 'HEAD^^'], setup.source)

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', olderAccepted], setup.directory)).toBe(
        olderAccepted
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
    })

    test('rebases a nested ordinary-note content edit', async () => {
      const workDir = ctx.getWorkDir()
      const source = buildSourceRepo(workDir)
      fs.mkdirSync(join(source, 'Nested'))
      fs.writeFileSync(
        join(source, 'Nested', 'Cell.md'),
        '---\ntype: Note\n---\n# Nested\n\nOriginal.\n'
      )
      fs.writeFileSync(
        join(source, 'other.md'),
        '---\ntype: Note\n---\n# Other\n\nAccepted body.\n'
      )
      runGit(['add', 'Nested/Cell.md', 'other.md'], source)
      runGit(['commit', '--quiet', '-m', 'add nested and other'], source)
      const directory = cloneAsBoundCheckout(
        workDir,
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      fs.writeFileSync(join(directory, 'Nested', 'Cell.md'), NESTED_NOTE)
      runGit(['add', 'Nested/Cell.md'], directory)
      runGit(['commit', '--quiet', '-m', 'unpublished nested edit'], directory)
      fs.writeFileSync(join(source, 'other.md'), OTHER_NOTE)
      runGit(['add', 'other.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted other-note edit'], source)
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      serveAcceptedBundle(ctx, source, 'rebase-nested')

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD^'], directory)).toBe(acceptedHead)
      expect(
        fs.readFileSync(join(directory, 'Nested', 'Cell.md'), 'utf8')
      ).toBe(NESTED_NOTE)
      expect(fs.readFileSync(join(directory, 'other.md'), 'utf8')).toBe(
        OTHER_NOTE
      )
    })
  })
}
