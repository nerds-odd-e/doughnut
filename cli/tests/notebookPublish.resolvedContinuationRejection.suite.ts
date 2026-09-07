import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
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
import { commitPortableFile } from './notebookPull.testHelpers.js'
import {
  bundleGetResponse,
  bundleMain,
  postCount,
  rejectionPost,
  stubFetchForSubmission,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'

const STALE_HEAD_MESSAGE =
  "expectedHead no longer matches the notebook's current accepted head."
const DRIFT_MESSAGE =
  "The notebook's current Portable content differs from accepted main; refresh the checkout before publishing."
const INVALID_YAML_MESSAGE = `${SPACED_NOTE_PATH} has invalid YAML frontmatter`
const BODY_INVALID_YAML =
  '---\ntype: Note\nbroken: [unterminated\n---\n# Note\n\nChosen sentence.\n'
const BODY_LATER_ACCEPTED =
  '---\ntype: Note\n---\n# Note\n\nLater accepted sentence.\n'

function resolvedWork(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    parent: runGit(['rev-parse', 'HEAD^'], directory),
    note: fs.readFileSync(join(directory, SPACED_NOTE_PATH), 'utf8'),
    status: runGit(['status', '--porcelain=v1'], directory),
  }
}

async function pullConflictThenContinue(
  workDir: string,
  chosenBytes: string
): Promise<{
  directory: string
  source: string
  acceptedBundle: string
  acceptedHead: string
  resolvedHead: string
}> {
  const setup = prepareConflictingSameNote(
    workDir,
    SPACED_NOTE_PATH,
    BODY_BASE,
    BODY_LOCAL,
    BODY_ACCEPTED
  )
  const acceptedBundle = join(workDir, 'accepted-B.bundle')
  bundleMain(setup.source, acceptedBundle)
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(bundleGetResponse(acceptedBundle))
  )
  await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
    ProcessExitForTest
  )
  continuePausedRebaseWithChosenBytes(
    setup.directory,
    SPACED_NOTE_PATH,
    chosenBytes
  )
  return {
    directory: setup.directory,
    source: setup.source,
    acceptedBundle,
    acceptedHead: setup.acceptedHead,
    resolvedHead: runGit(['rev-parse', 'HEAD'], setup.directory),
  }
}

export function describeNotebookPublishResolvedContinuationRejection(): void {
  describe('notebook publish (resolved same-note continuation retained on rejection)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-resolved-continuation-rejection-test-'
    )

    test('a stale expected-head after submitting a resolved child reports the conflict and leaves L′ intact', async () => {
      const continued = await pullConflictThenContinue(
        ctx.getWorkDir(),
        BODY_CHOSEN
      )
      const fetchMock = stubFetchForSubmission(
        continued.acceptedBundle,
        rejectionPost(409, STALE_HEAD_MESSAGE, 'RESOURCE_CONFLICT')
      )
      const afterResolution = resolvedWork(continued.directory)
      expect(afterResolution).toEqual({
        head: continued.resolvedHead,
        parent: continued.acceptedHead,
        note: BODY_CHOSEN,
        status: '',
      })
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(
        run(['notebook', 'publish', continued.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(resolvedWork(continued.directory)).toEqual(afterResolution)
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${STALE_HEAD_MESSAGE}`
      )
      expect(postCount(fetchMock)).toBe(1)
      expect(fetchMock.mock.calls).toHaveLength(2)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('remote advancement before ancestry download rejects without posting', async () => {
      const continued = await pullConflictThenContinue(
        ctx.getWorkDir(),
        BODY_CHOSEN
      )
      commitPortableFile(
        continued.source,
        SPACED_NOTE_PATH,
        BODY_LATER_ACCEPTED,
        'later accepted same-note edit'
      )
      const advancedBundle = join(ctx.getWorkDir(), 'accepted-advanced.bundle')
      bundleMain(continued.source, advancedBundle)
      const fetchMock = stubFetchWithBundleFile(advancedBundle)
      const afterResolution = resolvedWork(continued.directory)

      await expect(
        run(['notebook', 'publish', continued.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('single direct commit')
      )
      expect(postCount(fetchMock)).toBe(0)
      expect(fetchMock.mock.calls).toHaveLength(1)
      expect(resolvedWork(continued.directory)).toEqual(afterResolution)
    })

    test('a projection-drift rejection reports the drift and leaves L′ intact', async () => {
      const continued = await pullConflictThenContinue(
        ctx.getWorkDir(),
        BODY_CHOSEN
      )
      stubFetchForSubmission(
        continued.acceptedBundle,
        rejectionPost(409, DRIFT_MESSAGE, 'RESOURCE_CONFLICT')
      )
      const afterResolution = resolvedWork(continued.directory)

      await expect(
        run(['notebook', 'publish', continued.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${DRIFT_MESSAGE}`)
      expect(resolvedWork(continued.directory)).toEqual(afterResolution)
    })

    test('an invalid-content rejection reports the YAML error and leaves L′ intact', async () => {
      const continued = await pullConflictThenContinue(
        ctx.getWorkDir(),
        BODY_INVALID_YAML
      )
      stubFetchForSubmission(
        continued.acceptedBundle,
        rejectionPost(400, INVALID_YAML_MESSAGE, 'BINDING_ERROR')
      )
      const afterResolution = resolvedWork(continued.directory)
      expect(afterResolution.note).toBe(BODY_INVALID_YAML)

      await expect(
        run(['notebook', 'publish', continued.directory])
      ).rejects.toThrow(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${INVALID_YAML_MESSAGE}`
      )
      expect(resolvedWork(continued.directory)).toEqual(afterResolution)
    })
  })
}
