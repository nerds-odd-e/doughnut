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
  buildLfsSourceRepo,
  commitAttachmentHistory,
  commitPointerAttachment,
  installLfsPushIntercept,
  oid,
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

  function checkoutWithAcceptedAttachments(
    name: string,
    commits: number,
    attachmentsPerCommit: number
  ): string {
    const workDir = join(ctx.getWorkDir(), name)
    fs.mkdirSync(workDir)
    return prepareLfsPublishCheckout(
      workDir,
      'checkout',
      stubSuccessfulAcceptedHead(),
      (sourceRepoDir) =>
        commitAttachmentHistory(sourceRepoDir, commits, attachmentsPerCommit)
    ).dir
  }

  async function publish(dir: string) {
    const calls = installLfsPushIntercept(realSpawnSync)
    await run(['notebook', 'publish', dir])
    return calls
  }

  function pushedObjectIds(pushCalls: string[][]): string[] {
    const argv = pushCalls[0]!
    return argv.slice(argv.findIndex((arg) => arg.endsWith('/lfs')) + 1).sort()
  }

  describe('costs what the unpublished commits change', () => {
    async function publishNoteEditAfter(name: string, commits: number) {
      const dir = checkoutWithAcceptedAttachments(name, commits, 10)
      fs.writeFileSync(join(dir, 'note.md'), '# lfs notebook edited\n')
      runGit(['commit', '--quiet', '-am', 'edit note'], dir)
      return await publish(dir)
    }

    test('a note edit runs as many git processes after a long attachment history as after one commit, and uploads nothing', async () => {
      const short = await publishNoteEditAfter('short', 1)
      const long = await publishNoteEditAfter('long', 20)

      expect(long.gitCalls.length).toBe(short.gitCalls.length)
      expect([...short.pushCalls, ...long.pushCalls]).toEqual([])
    })

    test('uploads exactly the new files', async () => {
      const dir = checkoutWithAcceptedAttachments('new-files', 3, 3)
      commitPointerAttachment(realSpawnSync, dir, 'new1.bin', oid(101), 1, 'a')
      commitPointerAttachment(realSpawnSync, dir, 'new2.bin', oid(102), 2, 'b')

      const { pushCalls } = await publish(dir)

      expect(pushedObjectIds(pushCalls)).toEqual([oid(101), oid(102)])
    })

    test('uploads only the new version of one changed file', async () => {
      const dir = checkoutWithAcceptedAttachments('changed-file', 3, 3)
      commitPointerAttachment(realSpawnSync, dir, 'file1.bin', oid(201), 1, 'c')

      const { pushCalls } = await publish(dir)

      expect(pushedObjectIds(pushCalls)).toEqual([oid(201)])
    })
  })

  afterEach(() => {
    vi.mocked(childProcess.spawnSync).mockReset()
    vi.unstubAllGlobals()
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
        `${getApiConfig().apiBaseUrl}/api/notebooks/42/lfs`,
        OID_A,
        OID_B,
      ])
    )
    expect(runGit(['remote'], dir)).toBe('')
    expect(callOrder).toEqual(['lfs-push', 'bundle-post'])
    expect(postCount(fetchMock)).toBe(1)
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

    expect(pushedObjectIds(pushCalls)).toEqual([OID_B])
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
