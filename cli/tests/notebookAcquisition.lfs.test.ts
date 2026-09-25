import { describe, test, expect, vi } from 'vitest'
import * as childProcess from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { acquireNotebookGitCheckout } from '../src/commands/notebook/notebookAcquisition.js'
import {
  installAcquireNotebookGitCheckoutTest,
  stubBundleFetch,
  writeLfsCheckoutPointer,
} from './notebookAcquisition.testHelpers.js'
import { stagingDirsUnderTmp } from './notebookClone.testHelpers.js'

vi.mock('node:child_process', () => ({
  spawnSync: vi.fn(),
}))

vi.mock('node:fs', async () => {
  const actual = await vi.importActual<typeof fs>('node:fs')
  return { ...actual, renameSync: vi.fn(actual.renameSync) }
})

describe('acquireNotebookGitCheckout — Git LFS fill-in', () => {
  const ctx = installAcquireNotebookGitCheckoutTest()

  test('configures the LFS endpoint, fills in current files, then installs', async () => {
    const destinationPath = ctx.getDestinationPath()
    stubBundleFetch()
    let checkoutDir = ''
    const calls: string[][] = []
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[],
      options?: { env?: NodeJS.ProcessEnv }
    ) => {
      const argv = [...(args ?? [])]
      calls.push(argv)
      if (argv[0] === 'clone') {
        expect(options?.env?.GIT_LFS_SKIP_SMUDGE).toBe('1')
        checkoutDir = argv[3] as string
        writeLfsCheckoutPointer(
          checkoutDir,
          'payload.bin',
          'version https://git-lfs.github.com/spec/v1\noid sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nsize 4\n'
        )
      }
      const lfsIndex = argv.indexOf('lfs')
      if (lfsIndex >= 0 && argv[lfsIndex + 1] === 'pull') {
        fs.writeFileSync(join(checkoutDir, 'payload.bin'), 'abcd')
      }
      return {
        stdout: 'git-lfs/3.7.1\n',
        stderr: '',
        status: 0,
        error: undefined,
      }
    }) as typeof childProcess.spawnSync)

    await acquireNotebookGitCheckout(11, destinationPath)

    const { apiBaseUrl } = getApiConfig()
    const lfsUrl = `${apiBaseUrl}/api/notebooks/11/lfs`
    expect(calls).toEqual(
      expect.arrayContaining([
        ['clone', '--quiet', expect.any(String), expect.any(String)],
        ['-C', checkoutDir, 'config', '--local', 'donut.notebook-id', '11'],
        [
          '-C',
          checkoutDir,
          'config',
          '--local',
          'donut.api-origin',
          apiBaseUrl,
        ],
        ['-C', checkoutDir, 'config', '--local', 'lfs.url', lfsUrl],
        [
          '-C',
          checkoutDir,
          'config',
          '--local',
          'http.extraHeader',
          'Authorization: Bearer fake-bearer',
        ],
        ['-C', checkoutDir, 'lfs', 'install', '--local', '--skip-repo'],
        ['-C', checkoutDir, 'lfs', 'pull'],
        ['-C', checkoutDir, 'remote', 'remove', 'origin'],
      ])
    )
    expect(calls.filter((argv) => argv[2] === 'remote')).toEqual([
      ['-C', checkoutDir, 'remote', 'remove', 'origin'],
    ])
    expect(fs.readFileSync(join(destinationPath, 'payload.bin'), 'utf8')).toBe(
      'abcd'
    )
  })

  test('missing Git LFS leaves destination untouched and cleans staging', async () => {
    const destinationPath = ctx.getDestinationPath()
    stubBundleFetch()
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[]
    ) => {
      const argv = [...(args ?? [])]
      if (argv[0] === 'clone') {
        writeLfsCheckoutPointer(
          argv[3] as string,
          'payload.bin',
          'version https://git-lfs.github.com/spec/v1\noid sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nsize 1\n'
        )
        return { stdout: '', stderr: '', status: 0, error: undefined }
      }
      if (argv.includes('lfs')) {
        return {
          stdout: '',
          stderr: 'git: lfs is not a git command',
          status: 1,
          error: undefined,
        }
      }
      return { stdout: '', stderr: '', status: 0, error: undefined }
    }) as typeof childProcess.spawnSync)
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(12, destinationPath)
    ).rejects.toThrow(/Git LFS is required/i)

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
  })

  test('failed LFS fill-in leaves destination untouched and cleans staging', async () => {
    const destinationPath = ctx.getDestinationPath()
    stubBundleFetch()
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[]
    ) => {
      const argv = [...(args ?? [])]
      if (argv[0] === 'clone') {
        writeLfsCheckoutPointer(
          argv[3] as string,
          'payload.bin',
          'version https://git-lfs.github.com/spec/v1\noid sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\nsize 1\n'
        )
        return { stdout: '', stderr: '', status: 0, error: undefined }
      }
      const lfsIndex = argv.indexOf('lfs')
      if (lfsIndex >= 0 && argv[lfsIndex + 1] === 'pull') {
        return {
          stdout: '',
          stderr: 'Error downloading object: batch: Authentication required',
          status: 2,
          error: undefined,
        }
      }
      return {
        stdout: 'git-lfs/3.7.1\n',
        stderr: '',
        status: 0,
        error: undefined,
      }
    }) as typeof childProcess.spawnSync)
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(13, destinationPath)
    ).rejects.toThrow(
      /failed to download current notebook attachments via Git LFS/i
    )

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
  })
})
