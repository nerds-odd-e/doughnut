import * as childProcess from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { configureTestGitIdentity } from './notebookGit.testHelpers.js'
import {
  LFS_ATTRIBUTES,
  OID_A,
  OID_B,
  OID_OVER,
  THREE_MIB,
  TWENTY_MIB,
  buildLfsSourceRepo,
  commitPointerAttachment,
  installLfsPushIntercept,
  prepareLfsPublishCheckout,
  realSpawnSync,
  stubOrderedLfsThenBundleFetch,
  stubSuccessfulAcceptedHead,
} from './notebookPublish.lfs.testHelpers.js'
import {
  bundleMain,
  cloneAsBoundCheckout,
  postCount,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

vi.mock('node:child_process', async () => {
  const actual =
    await vi.importActual<typeof import('node:child_process')>(
      'node:child_process'
    )
  return { ...actual, spawnSync: vi.fn(actual.spawnSync) }
})

describe('notebook publish — LFS object upload before bundle submission', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-lfs-test-')

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

  test('accepted raw history converted to LFS does not block publishing a pointer', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = join(workDir, 'converted-source')
    fs.mkdirSync(sourceRepoDir)
    runGit(['init', '--quiet', '-b', 'main'], sourceRepoDir)
    configureTestGitIdentity(sourceRepoDir)
    fs.writeFileSync(join(sourceRepoDir, 'note.md'), '# raw\n')
    fs.writeFileSync(
      join(sourceRepoDir, 'first.png'),
      Buffer.from([0x89, 0x50])
    )
    runGit(['add', 'note.md', 'first.png'], sourceRepoDir)
    runGit(['commit', '--quiet', '-m', 'raw'], sourceRepoDir)
    fs.writeFileSync(join(sourceRepoDir, '.gitattributes'), LFS_ATTRIBUTES)
    runGit(['add', '.gitattributes'], sourceRepoDir)
    commitPointerAttachment(
      realSpawnSync,
      sourceRepoDir,
      'first.png',
      OID_A,
      2,
      'convert to LFS'
    )
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchForSubmission(bundleFile, stubSuccessfulAcceptedHead())
    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'converted-checkout'
    )
    commitPointerAttachment(realSpawnSync, dir, 'second.png', OID_B, 64, 'v2')

    await run(['notebook', 'publish', dir])

    expect(pushCalls).toHaveLength(1)
    expect(pushCalls[0]).toEqual(expect.arrayContaining([OID_A, OID_B]))
  })

  test('publishes a note and an attachment under a non-ASCII folder', async () => {
    const { pushCalls } = installLfsPushIntercept(realSpawnSync)
    const { dir, fetchMock } = prepareLfsPublishCheckout(
      ctx.getWorkDir(),
      'lfs-non-ascii',
      stubSuccessfulAcceptedHead()
    )
    fs.mkdirSync(join(dir, '例文'))
    fs.writeFileSync(join(dir, '例文', 'A.md'), '# A\n')
    runGit(['add', '例文/A.md'], dir)
    commitPointerAttachment(realSpawnSync, dir, '例文/圖.png', OID_A, 64, 'v1')

    await run(['notebook', 'publish', dir])

    expect(pushCalls).toHaveLength(1)
    expect(pushCalls[0]).toContain(OID_A)
    expect(postCount(fetchMock)).toBe(1)
  })
})
