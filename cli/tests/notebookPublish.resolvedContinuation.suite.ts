import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, type vi } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  BODY_ACCEPTED,
  BODY_BASE,
  BODY_CHOSEN,
  BODY_LOCAL,
  SPACED_NOTE_PATH,
  continuePausedRebaseWithChosenBytes,
  prepareConflictingSameNote,
} from './notebookPull.conflict.testHelpers.js'
import {
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  bundleMain,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

function postCount(fetchMock: ReturnType<typeof vi.fn>): number {
  return fetchMock.mock.calls.filter(
    ([, init]: [unknown, { method?: string } | undefined]) =>
      init?.method === 'POST'
  ).length
}

export function describeNotebookPublishResolvedContinuation(): void {
  describe('notebook publish (resolved same-note continuation)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-publish-resolved-continuation-test-'
    )

    test('submits the continued child as the bundle head with accepted main as expected head', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        SPACED_NOTE_PATH,
        BODY_BASE,
        BODY_LOCAL,
        BODY_ACCEPTED
      )
      serveAcceptedBundle(ctx, setup.source, 'publish-continued-child')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(postCount(ctx.getFetchMock())).toBe(0)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)

      continuePausedRebaseWithChosenBytes(
        setup.directory,
        SPACED_NOTE_PATH,
        BODY_CHOSEN
      )
      const resolvedHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      const acceptedBundle = join(ctx.getWorkDir(), 'accepted-B.bundle')
      bundleMain(setup.source, acceptedBundle)
      const fetchMock = stubFetchForSubmission(acceptedBundle, {
        status: 200,
        ok: true,
        text: () => Promise.resolve(resolvedHead),
      })

      await run(['notebook', 'publish', setup.directory])

      const postCall = fetchMock.mock.calls.find(
        ([, init]: [unknown, { method?: string } | undefined]) =>
          init?.method === 'POST'
      )
      expect(postCall).toBeDefined()
      const [url, init] = postCall as [string, { method: string; body: Buffer }]
      expect(url).toContain(
        `/notebooks/42/git-bundle?expectedHead=${encodeURIComponent(setup.acceptedHead)}`
      )
      expect(postCount(fetchMock)).toBe(1)
      expect(Buffer.isBuffer(init.body)).toBe(true)

      const postedBundleFile = join(ctx.getWorkDir(), 'posted.bundle')
      fs.writeFileSync(postedBundleFile, init.body)
      const clonedDir = join(ctx.getWorkDir(), 'posted-clone')
      runGit(
        [
          '-c',
          'init.defaultBranch=ci-default',
          'clone',
          '--quiet',
          '--branch',
          'main',
          postedBundleFile,
          clonedDir,
        ],
        ctx.getWorkDir()
      )
      expect(runGit(['rev-parse', 'main'], clonedDir)).toBe(resolvedHead)
      expect(runGit(['rev-parse', 'main^'], clonedDir)).toBe(setup.acceptedHead)
    })
  })
}
