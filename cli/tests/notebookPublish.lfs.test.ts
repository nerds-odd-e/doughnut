import * as childProcess from 'node:child_process'
import type { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { configureTestGitIdentity } from './notebookGit.testHelpers.js'
import {
  OID_A,
  OID_B,
  OID_OVER,
  THREE_MIB,
  TWENTY_MIB,
  buildLfsSourceRepo,
  commitPointerAttachment,
  installLfsPushIntercept,
  prepareLfsPublishCheckout,
  stubOrderedLfsThenBundleFetch,
  stubSuccessfulAcceptedHead,
} from './notebookPublish.lfs.testHelpers.js'
import {
  bundleMain,
  cloneAsBoundCheckout,
  localGitObservation,
  postCount,
  rejectionPost,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

vi.mock('node:child_process', async () => {
  const actual =
    await vi.importActual<typeof import('node:child_process')>(
      'node:child_process'
    )
  return {
    ...actual,
    spawnSync: vi.fn(actual.spawnSync),
  }
})

let realSpawnSync: typeof spawnSync

describe('notebook publish — LFS object upload before bundle submission', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-lfs-test-')

  beforeAll(async () => {
    const actual =
      await vi.importActual<typeof import('node:child_process')>(
        'node:child_process'
      )
    realSpawnSync = actual.spawnSync
  })

  beforeEach(() => {
    vi.mocked(childProcess.spawnSync).mockImplementation(
      realSpawnSync as typeof spawnSync
    )
  })

  afterEach(() => {
    vi.mocked(childProcess.spawnSync).mockReset()
    vi.unstubAllGlobals()
  })

  test('raw checkout publishes without Git LFS upload', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = join(workDir, 'raw-source')
    fs.mkdirSync(sourceRepoDir)
    runGit(['init', '--quiet', '-b', 'main'], sourceRepoDir)
    configureTestGitIdentity(sourceRepoDir)
    fs.writeFileSync(join(sourceRepoDir, 'note.md'), '# raw\n')
    runGit(['add', 'note.md'], sourceRepoDir)
    runGit(['commit', '--quiet', '-m', 'raw'], sourceRepoDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    const fetchMock = stubFetchForSubmission(
      bundleFile,
      stubSuccessfulAcceptedHead()
    )
    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'raw-checkout'
    )
    fs.writeFileSync(join(dir, 'note.md'), '# edited\n')
    runGit(['add', 'note.md'], dir)
    runGit(['commit', '--quiet', '-m', 'edit'], dir)

    await run(['notebook', 'publish', dir])

    expect(pushCalls).toEqual([])
    expect(postCount(fetchMock)).toBe(1)
  })

  test('uploads required in-limit objects before submitting the bundle', async () => {
    const callOrder: string[] = []
    const { pushCalls } = installLfsPushIntercept(realSpawnSync, {
      onPush: () => callOrder.push('lfs-push'),
    })
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildLfsSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    const fetchMock = stubOrderedLfsThenBundleFetch(bundleFile, callOrder)
    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'lfs-checkout'
    )
    commitPointerAttachment(realSpawnSync, dir, 'payload.bin', OID_A, 128, 'v1')
    commitPointerAttachment(realSpawnSync, dir, 'payload.bin', OID_B, 256, 'v2')

    await run(['notebook', 'publish', dir])

    expect(pushCalls).toHaveLength(1)
    expect(pushCalls[0]).toEqual(
      expect.arrayContaining([
        'lfs',
        'push',
        '--object-id',
        'origin',
        OID_A,
        OID_B,
      ])
    )
    expect(callOrder).toEqual(['lfs-push', 'bundle-post'])
    expect(postCount(fetchMock)).toBe(1)
  })

  test('does not upload a new oversized intermediate-only object', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const { dir } = prepareLfsPublishCheckout(
      ctx.getWorkDir(),
      'lfs-over',
      stubSuccessfulAcceptedHead()
    )
    commitPointerAttachment(
      realSpawnSync,
      dir,
      'payload.bin',
      OID_OVER,
      TWENTY_MIB,
      'over'
    )
    commitPointerAttachment(
      realSpawnSync,
      dir,
      'payload.bin',
      OID_B,
      THREE_MIB,
      'tip'
    )

    await run(['notebook', 'publish', dir])

    expect(pushCalls).toHaveLength(1)
    expect(pushCalls[0]).toContain(OID_B)
    expect(pushCalls[0]).not.toContain(OID_OVER)
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
