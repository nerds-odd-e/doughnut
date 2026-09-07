import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  BODY_ACCEPTED,
  BODY_BASE,
  BODY_CHOSEN,
  BODY_LOCAL,
  NESTED_YAML_PATH,
  SPACED_NOTE_PATH,
  YAML_ACCEPTED,
  YAML_BASE,
  YAML_CHOSEN,
  YAML_LOCAL,
  continuePausedRebaseWithChosenBytes,
  prepareConflictingSameNote,
  skipPausedRebaseWithChosenBytes,
} from './notebookPull.conflict.testHelpers.js'
import {
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

export function describeNotebookPullResolvedContinuation(): void {
  describe('notebook pull (resolved same-note continuation)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-resolved-continuation-test-'
    )

    test('continues a paused same-note conflict into one child of accepted main', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        SPACED_NOTE_PATH,
        BODY_BASE,
        BODY_LOCAL,
        BODY_ACCEPTED
      )
      const originalAuthor = runGit(
        ['log', '-1', '--format=%an <%ae>', setup.localTip],
        setup.directory
      )
      const originalMessage = runGit(
        ['log', '-1', '--format=%s', setup.localTip],
        setup.directory
      )
      serveAcceptedBundle(ctx, setup.source, 'continue-chosen-body')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)

      continuePausedRebaseWithChosenBytes(
        setup.directory,
        SPACED_NOTE_PATH,
        BODY_CHOSEN
      )

      const resolvedHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', 'main'], setup.directory)).toBe(resolvedHead)
      expect(
        fs.readFileSync(join(setup.directory, SPACED_NOTE_PATH), 'utf8')
      ).toBe(BODY_CHOSEN)
      expect(runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)).toBe(
        originalAuthor
      )
      expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
        originalMessage
      )
      expect(
        fs.existsSync(
          join(
            setup.directory,
            runGit(['rev-parse', '--git-path', 'rebase-merge'], setup.directory)
          )
        )
      ).toBe(false)

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(resolvedHead)
      expect(ctx.getLogSpy().mock.calls.at(-1)).toEqual([
        `Unpublished local commit is already based on the accepted history. Local head: ${resolvedHead}. Accepted head: ${setup.acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`,
      ])
      expect(
        ctx
          .getFetchMock()
          .mock.calls.some(
            (call) =>
              (call[1] as { method?: string } | undefined)?.method === 'POST'
          )
      ).toBe(false)
    })

    test('keeps the chosen valid YAML after continuing a nested frontmatter conflict', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        NESTED_YAML_PATH,
        YAML_BASE,
        YAML_LOCAL,
        YAML_ACCEPTED
      )
      serveAcceptedBundle(ctx, setup.source, 'continue-chosen-yaml')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      continuePausedRebaseWithChosenBytes(
        setup.directory,
        NESTED_YAML_PATH,
        YAML_CHOSEN
      )

      expect(
        fs.readFileSync(join(setup.directory, NESTED_YAML_PATH), 'utf8')
      ).toBe(YAML_CHOSEN)
    })

    test('an explicit skip of the accepted-side pause leaves no unpublished child', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        SPACED_NOTE_PATH,
        BODY_BASE,
        BODY_LOCAL,
        BODY_ACCEPTED
      )
      serveAcceptedBundle(ctx, setup.source, 'skip-accepted-side')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      skipPausedRebaseWithChosenBytes(
        setup.directory,
        SPACED_NOTE_PATH,
        BODY_ACCEPTED
      )

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(ctx.getLogSpy().mock.calls.at(-1)).toEqual([
        `Notebook unchanged. Accepted head: ${setup.acceptedHead}`,
      ])
    })
  })
}
