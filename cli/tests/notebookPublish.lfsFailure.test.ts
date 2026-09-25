import * as childProcess from 'node:child_process'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  OID_A,
  commitPointerAttachment,
  hashObject,
  installLfsPushIntercept,
  prepareLfsPublishCheckout,
  realSpawnSync,
  stubSuccessfulAcceptedHead,
} from './notebookPublish.lfs.testHelpers.js'
import {
  localGitObservation,
  postCount,
  rejectionPost,
} from './notebookPublish.testHelpers.js'

vi.mock('node:child_process', async () => {
  const actual =
    await vi.importActual<typeof import('node:child_process')>(
      'node:child_process'
    )
  return { ...actual, spawnSync: vi.fn(actual.spawnSync) }
})

describe('notebook publish — LFS refusals and failures keep local state', () => {
  const ctx = installNotebookCliRunFixture(
    'donut-cli-publish-lfs-failure-test-'
  )

  afterEach(() => {
    vi.mocked(childProcess.spawnSync).mockReset()
    vi.unstubAllGlobals()
  })

  test('rejects publishing a raw attachment', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const { dir, fetchMock } = prepareLfsPublishCheckout(
      ctx.getWorkDir(),
      'lfs-raw-publish',
      stubSuccessfulAcceptedHead()
    )
    const blob = hashObject(realSpawnSync, dir, Buffer.from([0x89, 0x50]))
    runGit(
      ['update-index', '--add', '--cacheinfo', `100644,${blob},raw.png`],
      dir
    )
    runGit(['commit', '--quiet', '-m', 'raw'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining(
        'Attachment "raw.png" must be a Git LFS pointer or empty file.'
      )
    )
    expect(pushCalls).toEqual([])
    expect(postCount(fetchMock)).toBe(0)
  })

  test('failed LFS upload preserves local refs and files and skips bundle POST', async () => {
    installLfsPushIntercept(realSpawnSync, { failPush: true })
    const { dir, fetchMock } = prepareLfsPublishCheckout(
      ctx.getWorkDir(),
      'lfs-fail-upload',
      stubSuccessfulAcceptedHead()
    )
    commitPointerAttachment(realSpawnSync, dir, 'payload.bin', OID_A, 64, 'v1')
    const before = localGitObservation(dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining(
        'failed to upload notebook attachments via Git LFS'
      )
    )
    expect(localGitObservation(dir)).toEqual(before)
    expect(postCount(fetchMock)).toBe(0)
  })

  test('failed bundle submission after upload preserves local refs and files', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const { dir } = prepareLfsPublishCheckout(
      ctx.getWorkDir(),
      'lfs-fail-post',
      rejectionPost(
        400,
        'note.md has invalid YAML frontmatter',
        'BINDING_ERROR'
      )
    )
    commitPointerAttachment(realSpawnSync, dir, 'payload.bin', OID_A, 64, 'v1')
    const before = localGitObservation(dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      'donut: note.md has invalid YAML frontmatter'
    )
    expect(pushCalls).toHaveLength(1)
    expect(localGitObservation(dir)).toEqual(before)
  })
})
