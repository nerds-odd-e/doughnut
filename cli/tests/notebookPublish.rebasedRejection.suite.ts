import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  bundleGetResponse,
  bundleMain,
  stubFetchForSubmission,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'
import {
  LATER_OTHER_NOTE,
  LOCAL_NOTE,
  OTHER_NOTE,
  prepareEligibleDivergence,
} from './notebookPull.rebase.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'

const STALE_HEAD_MESSAGE =
  "expectedHead no longer matches the notebook's current accepted head."
const DRIFT_MESSAGE =
  "The notebook's current Portable content differs from accepted main; refresh the checkout before publishing."

function conflictPost(message: string): {
  status: number
  ok: boolean
  text: () => Promise<string>
} {
  return {
    status: 409,
    ok: false,
    text: () =>
      Promise.resolve(
        JSON.stringify({ message, errorType: 'RESOURCE_CONFLICT' })
      ),
  }
}

function postCount(fetchMock: ReturnType<typeof vi.fn>): number {
  return fetchMock.mock.calls.filter(
    ([, init]: [unknown, { method?: string } | undefined]) =>
      init?.method === 'POST'
  ).length
}

function rebasedWork(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    parent: runGit(['rev-parse', 'HEAD^'], directory),
    note: fs.readFileSync(join(directory, 'note.md'), 'utf8'),
    other: fs.readFileSync(join(directory, 'other.md'), 'utf8'),
    status: runGit(['status', '--porcelain=v1'], directory),
  }
}

async function pullEligibleRebase(workDir: string): Promise<{
  directory: string
  source: string
  acceptedBundle: string
  originalLocalTip: string
  acceptedHead: string
  rebasedHead: string
}> {
  const setup = prepareEligibleDivergence(workDir, { remoteEdits: 1 })
  const acceptedBundle = join(workDir, 'accepted-B.bundle')
  bundleMain(setup.source, acceptedBundle)
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(bundleGetResponse(acceptedBundle))
  )
  await run(['notebook', 'pull', setup.directory])
  const rebasedHead = runGit(['rev-parse', 'HEAD'], setup.directory)
  return {
    directory: setup.directory,
    source: setup.source,
    acceptedBundle,
    originalLocalTip: setup.localTip,
    acceptedHead: setup.acceptedHead,
    rebasedHead,
  }
}

export function describeNotebookPublishRebasedRejection(): void {
  describe('notebook publish (rebased unpublished work retained on rejection)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-rebased-rejection-test-'
    )

    test('a stale expected-head after submitting a rebased child reports the conflict and leaves L′ intact', async () => {
      const workDir = ctx.getWorkDir()
      const pulled = await pullEligibleRebase(workDir)
      const fetchMock = stubFetchForSubmission(
        pulled.acceptedBundle,
        conflictPost(STALE_HEAD_MESSAGE)
      )
      const afterRebase = rebasedWork(pulled.directory)
      expect(afterRebase).toEqual({
        head: pulled.rebasedHead,
        parent: pulled.acceptedHead,
        note: LOCAL_NOTE,
        other: OTHER_NOTE,
        status: '',
      })
      expect(afterRebase.head).not.toBe(pulled.originalLocalTip)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(
        run(['notebook', 'publish', pulled.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(rebasedWork(pulled.directory)).toEqual(afterRebase)
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${STALE_HEAD_MESSAGE}`
      )
      expect(postCount(fetchMock)).toBe(1)
      expect(fetchMock.mock.calls).toHaveLength(2)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('remote advancement before ancestry download rejects without posting', async () => {
      const workDir = ctx.getWorkDir()
      const pulled = await pullEligibleRebase(workDir)
      fs.writeFileSync(join(pulled.source, 'other.md'), LATER_OTHER_NOTE)
      runGit(['add', 'other.md'], pulled.source)
      runGit(
        ['commit', '--quiet', '-m', 'later accepted other-note edit'],
        pulled.source
      )
      const advancedBundle = join(workDir, 'accepted-advanced.bundle')
      bundleMain(pulled.source, advancedBundle)
      const fetchMock = stubFetchWithBundleFile(advancedBundle)
      const afterRebase = rebasedWork(pulled.directory)

      await expect(
        run(['notebook', 'publish', pulled.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('single direct commit')
      )
      expect(postCount(fetchMock)).toBe(0)
      expect(fetchMock.mock.calls).toHaveLength(1)
      expect(rebasedWork(pulled.directory)).toEqual(afterRebase)
    })

    test('a projection-drift rejection reports the drift', async () => {
      const pulled = await pullEligibleRebase(ctx.getWorkDir())
      stubFetchForSubmission(pulled.acceptedBundle, conflictPost(DRIFT_MESSAGE))

      await expect(
        run(['notebook', 'publish', pulled.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${DRIFT_MESSAGE}`)
    })
  })
}
