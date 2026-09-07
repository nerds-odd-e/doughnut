import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  BODY_ACCEPTED,
  BODY_BASE,
  BODY_LOCAL,
  NESTED_YAML_PATH,
  SPACED_NOTE_PATH,
  YAML_ACCEPTED,
  YAML_BASE,
  YAML_LOCAL,
  prepareConflictingSameNote,
} from './notebookPull.conflict.testHelpers.js'
import {
  cloneWithLocalNoteEdit,
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

const MERGEABLE_LOCAL =
  '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nShared closing.\n'
const MERGEABLE_ACCEPTED =
  '---\ntype: Note\n---\n# Note\n\nShared opening.\n\nRemote closing.\n'

function pullCreatedConflictObservation(
  directory: string,
  relativePath: string
) {
  const rebaseMergePath = runGit(
    ['rev-parse', '--git-path', 'rebase-merge'],
    directory
  )
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    main: runGit(['rev-parse', 'main'], directory),
    unmerged: runGit(['ls-files', '-u'], directory),
    ours: runGit(['show', `:2:${relativePath}`], directory),
    theirs: runGit(['show', `:3:${relativePath}`], directory),
    rebaseMerge: fs.existsSync(join(directory, rebaseMergePath)),
  }
}

function quotedGitAdd(relativePath: string): string {
  return `git add -- '${relativePath}'`
}

export function describeNotebookPullConflict(): void {
  describe('notebook pull (same-note text conflict pause)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-conflict-test-'
    )

    test('pauses with native Git guidance when the same body line conflicts', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        SPACED_NOTE_PATH,
        BODY_BASE,
        BODY_LOCAL,
        BODY_ACCEPTED
      )
      serveAcceptedBundle(ctx, setup.source, 'body-line-conflict')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      const error = String(ctx.getErrorSpy().mock.calls[0]?.[0])
      expect(error).toContain(`"${SPACED_NOTE_PATH}"`)
      expect(error).toContain('git status')
      expect(error).toContain(quotedGitAdd(SPACED_NOTE_PATH))
      expect(error).toContain('git rebase --continue')
      expect(error).toContain('git rebase --abort')
      expect(error).toContain(`donut notebook publish ${setup.directory}`)
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)

      const paused = pullCreatedConflictObservation(
        setup.directory,
        SPACED_NOTE_PATH
      )
      expect(paused.rebaseMerge).toBe(true)
      expect(paused.unmerged).toContain(SPACED_NOTE_PATH)
      expect(paused.head).toBe(setup.acceptedHead)
      expect(paused.main).toBe(setup.localTip)
      expect(paused.ours).toBe(BODY_ACCEPTED.trim())
      expect(paused.theirs).toBe(BODY_LOCAL.trim())
      expect(ctx.getFetchMock()).toHaveBeenCalledTimes(1)
      expect(ctx.getFetchMock().mock.calls[0]?.[0]).toContain('/git-bundle')
      expect(
        ctx
          .getFetchMock()
          .mock.calls.some(
            (call) =>
              (call[1] as { method?: string } | undefined)?.method === 'POST'
          )
      ).toBe(false)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          'Finish or abort the active Git operation before receiving'
        )
      )
      expect(
        pullCreatedConflictObservation(setup.directory, SPACED_NOTE_PATH)
      ).toEqual(paused)

      await expect(
        run(['notebook', 'publish', setup.directory])
      ).rejects.toThrow(ProcessExitForTest)
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          'Finish or abort the active Git operation before publishing'
        )
      )
      expect(
        pullCreatedConflictObservation(setup.directory, SPACED_NOTE_PATH)
      ).toEqual(paused)
      expect(ctx.getFetchMock()).toHaveBeenCalledTimes(1)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('names a nested YAML-key conflict in the same pause guidance', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        NESTED_YAML_PATH,
        YAML_BASE,
        YAML_LOCAL,
        YAML_ACCEPTED
      )
      serveAcceptedBundle(ctx, setup.source, 'nested-yaml-conflict')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      const error = String(ctx.getErrorSpy().mock.calls[0]?.[0])
      expect(error).toContain(`"${NESTED_YAML_PATH}"`)
      expect(error).toContain(quotedGitAdd(NESTED_YAML_PATH))
    })

    test('keeps the rebase cause when Git did not pause with unmerged stages', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        BODY_BASE,
        MERGEABLE_LOCAL
      )
      commitPortableFile(
        setup.source,
        'note.md',
        MERGEABLE_ACCEPTED,
        'accepted disjoint closing'
      )
      const hook = join(setup.directory, '.git', 'hooks', 'pre-rebase')
      fs.writeFileSync(hook, '#!/bin/sh\necho hook-stopped-rebase\nexit 1\n')
      fs.chmodSync(hook, 0o755)
      serveAcceptedBundle(ctx, setup.source, 'rebase-hook-failure')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      const error = String(ctx.getErrorSpy().mock.calls[0]?.[0])
      expect(error).toContain(
        'failed to rebase the unpublished local commit onto the accepted head'
      )
      expect(error).toContain('hook-stopped-rebase')
      expect(error).not.toContain('git rebase --continue')
    })
  })
}
