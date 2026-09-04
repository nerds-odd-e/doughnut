import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import * as childProcess from 'node:child_process'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { acquireNotebookGitCheckout } from '../src/commands/notebook/notebookAcquisition.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

vi.mock('node:child_process', () => ({
  spawnSync: vi.fn(),
}))

vi.mock('node:fs', async () => {
  const actual = await vi.importActual<typeof fs>('node:fs')
  return { ...actual, renameSync: vi.fn(actual.renameSync) }
})

const STAGING_PREFIX = 'donut-notebook-clone-'

function stagingDirsUnderTmp(): string[] {
  return fs
    .readdirSync(tmpdir())
    .filter((name) => name.startsWith(STAGING_PREFIX))
}

describe('acquireNotebookGitCheckout', () => {
  let savedConfigDir: string | undefined
  let configDir: string
  let destinationParent: string
  let destinationPath: string

  beforeEach(() => {
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    configDir = tempConfigWithToken()
    process.env.DONUT_CONFIG_DIR = configDir
    destinationParent = fs.mkdtempSync(join(tmpdir(), 'donut-notebook-dest-'))
    destinationPath = join(destinationParent, 'notebook-checkout')
  })

  afterEach(() => {
    if (savedConfigDir === undefined) delete process.env.DONUT_CONFIG_DIR
    else process.env.DONUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
    fs.rmSync(destinationParent, { recursive: true, force: true })
    vi.unstubAllGlobals()
    vi.mocked(childProcess.spawnSync).mockReset()
    vi.mocked(fs.renameSync).mockClear()
  })

  test('binary download failure (non-OK response) leaves destination untouched and cleans staging', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 404 })
    )
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(1, destinationPath)
    ).rejects.toThrow(/resource was not found/i)

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
    expect(childProcess.spawnSync).not.toHaveBeenCalled()
  })

  test('binary download failure (fetch rejects) leaves destination untouched and cleans staging', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new TypeError('fetch failed'))
    )
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(2, destinationPath)
    ).rejects.toThrow()

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
  })

  test('missing stored access token leaves destination untouched and cleans staging', async () => {
    fs.rmSync(join(configDir, 'access-tokens.json'))
    vi.stubGlobal('fetch', vi.fn())
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(3, destinationPath)
    ).rejects.toThrow(/access token/i)

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
    expect(fetch).not.toHaveBeenCalled()
  })

  test('git executable missing leaves destination untouched and cleans staging', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
      })
    )
    vi.mocked(childProcess.spawnSync).mockReturnValue({
      stdout: '',
      stderr: '',
      status: null,
      error: Object.assign(new Error('spawn git ENOENT'), { code: 'ENOENT' }),
    } as ReturnType<typeof childProcess.spawnSync>)
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(4, destinationPath)
    ).rejects.toThrow(/git/i)

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
  })

  test('git clone non-zero exit leaves destination untouched and cleans staging', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
      })
    )
    vi.mocked(childProcess.spawnSync).mockReturnValue({
      stdout: '',
      stderr: 'fatal: not a valid bundle file',
      status: 128,
      error: undefined,
    } as ReturnType<typeof childProcess.spawnSync>)
    const before = stagingDirsUnderTmp()

    await expect(
      acquireNotebookGitCheckout(5, destinationPath)
    ).rejects.toThrow(/clone/i)

    expect(fs.existsSync(destinationPath)).toBe(false)
    expect(stagingDirsUnderTmp()).toEqual(before)
  })

  test('happy path downloads, clones, and installs the checkout at the destination', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
      })
    )
    let checkoutDir = ''
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[]
    ) => {
      if (args?.[0] === 'clone') {
        checkoutDir = args[3] as string
        fs.mkdirSync(checkoutDir, { recursive: true })
        fs.writeFileSync(join(checkoutDir, 'README.md'), '# notebook')
      }
      return { stdout: '', stderr: '', status: 0, error: undefined }
    }) as typeof childProcess.spawnSync)
    const before = stagingDirsUnderTmp()

    await acquireNotebookGitCheckout(6, destinationPath)

    expect(fs.existsSync(join(destinationPath, 'README.md'))).toBe(true)
    expect(stagingDirsUnderTmp()).toEqual(before)

    const { apiBaseUrl } = getApiConfig()
    expect(childProcess.spawnSync).toHaveBeenCalledWith(
      'git',
      ['-C', checkoutDir, 'config', '--local', 'donut.notebook-id', '6'],
      { encoding: 'utf8' }
    )
    expect(childProcess.spawnSync).toHaveBeenCalledWith(
      'git',
      ['-C', checkoutDir, 'config', '--local', 'donut.api-origin', apiBaseUrl],
      { encoding: 'utf8' }
    )
    expect(childProcess.spawnSync).toHaveBeenCalledWith(
      'git',
      ['-C', checkoutDir, 'remote', 'remove', 'origin'],
      { encoding: 'utf8' }
    )
  })

  test('git config recording failure surfaces an error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
      })
    )
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[]
    ) => {
      if (args?.[0] === 'clone') {
        const targetDir = args[3] as string
        fs.mkdirSync(targetDir, { recursive: true })
        fs.writeFileSync(join(targetDir, 'README.md'), '# notebook')
        return { stdout: '', stderr: '', status: 0, error: undefined }
      }
      return {
        stdout: '',
        stderr: 'fatal: could not lock config file',
        status: 1,
        error: undefined,
      }
    }) as typeof childProcess.spawnSync)

    await expect(
      acquireNotebookGitCheckout(9, destinationPath)
    ).rejects.toThrow(/failed to record local Git config/)

    expect(fs.existsSync(destinationPath)).toBe(false)
  })

  test('falls back to copy+remove when rename fails with EXDEV (cross-device destination)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('bundle-bytes').buffer),
      })
    )
    vi.mocked(childProcess.spawnSync).mockImplementation(((
      _cmd: string,
      args?: readonly string[]
    ) => {
      if (args?.[0] === 'clone') {
        const targetDir = args[3] as string
        fs.mkdirSync(targetDir, { recursive: true })
        fs.writeFileSync(join(targetDir, 'README.md'), '# notebook')
        fs.mkdirSync(join(targetDir, 'nested'))
        fs.writeFileSync(join(targetDir, 'nested', 'note.md'), 'nested content')
      }
      return { stdout: '', stderr: '', status: 0, error: undefined }
    }) as typeof childProcess.spawnSync)
    vi.mocked(fs.renameSync).mockImplementationOnce(() => {
      throw Object.assign(new Error('EXDEV: cross-device link not permitted'), {
        code: 'EXDEV',
      })
    })
    const before = stagingDirsUnderTmp()

    await acquireNotebookGitCheckout(8, destinationPath)

    expect(fs.readFileSync(join(destinationPath, 'README.md'), 'utf8')).toBe(
      '# notebook'
    )
    expect(
      fs.readFileSync(join(destinationPath, 'nested', 'note.md'), 'utf8')
    ).toBe('nested content')
    expect(stagingDirsUnderTmp()).toEqual(before)
  })

  test('refuses to overwrite an already-existing destination', async () => {
    fs.mkdirSync(destinationPath)
    fs.writeFileSync(join(destinationPath, 'sentinel.txt'), 'pre-existing')
    vi.stubGlobal('fetch', vi.fn())

    await expect(
      acquireNotebookGitCheckout(7, destinationPath)
    ).rejects.toThrow(/already exists/)

    expect(fs.readFileSync(join(destinationPath, 'sentinel.txt'), 'utf8')).toBe(
      'pre-existing'
    )
    expect(childProcess.spawnSync).not.toHaveBeenCalled()
    expect(fetch).not.toHaveBeenCalled()
  })
})
