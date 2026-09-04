import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import { execFileSync } from 'node:child_process'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

class ProcessExitForTest extends Error {
  readonly code: number | undefined
  constructor(code?: number) {
    super(`process.exit(${code})`)
    this.name = 'ProcessExitForTest'
    this.code = code
  }
}

function runGit(args: string[], cwd: string): string {
  return execFileSync('git', args, { cwd, encoding: 'utf8' }).trim()
}

describe('notebook clone (CLI routing, real Git checkout)', () => {
  let savedConfigDir: string | undefined
  let configDir: string
  let workDir: string
  let sourceRepoDir: string
  let destinationPath: string
  let errorSpy: ReturnType<typeof vi.spyOn>
  let logSpy: ReturnType<typeof vi.spyOn>
  let exitSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    savedConfigDir = process.env.DONUT_CONFIG_DIR
    configDir = tempConfigWithToken()
    process.env.DONUT_CONFIG_DIR = configDir

    workDir = fs.mkdtempSync(join(tmpdir(), 'donut-notebook-clone-cli-'))
    sourceRepoDir = join(workDir, 'source')
    destinationPath = join(workDir, 'destination')

    fs.mkdirSync(sourceRepoDir)
    runGit(['init', '--quiet', '-b', 'main'], sourceRepoDir)
    runGit(['config', 'user.email', 'test@example.com'], sourceRepoDir)
    runGit(['config', 'user.name', 'Test'], sourceRepoDir)
    fs.writeFileSync(join(sourceRepoDir, 'note.md'), '# hello notebook\n')
    runGit(['add', 'note.md'], sourceRepoDir)
    runGit(
      ['commit', '--quiet', '-m', 'initial notebook commit'],
      sourceRepoDir
    )

    errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    exitSpy = vi.spyOn(process, 'exit').mockImplementation(((code?: number) => {
      throw new ProcessExitForTest(code)
    }) as typeof process.exit)
  })

  afterEach(() => {
    if (savedConfigDir === undefined) delete process.env.DONUT_CONFIG_DIR
    else process.env.DONUT_CONFIG_DIR = savedConfigDir
    fs.rmSync(configDir, { recursive: true, force: true })
    fs.rmSync(workDir, { recursive: true, force: true })
    vi.unstubAllGlobals()
    errorSpy.mockRestore()
    logSpy.mockRestore()
    exitSpy.mockRestore()
  })

  test('clone downloads the accepted bundle and checks it out as a clean single-commit main branch', async () => {
    const bundleFile = join(workDir, 'notebook.bundle')
    // Include HEAD so the clone checks out "main" regardless of the machine's own
    // `init.defaultBranch` setting, matching how the real backend bundle is built
    // (NotebookGitBundleWriter).
    runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], sourceRepoDir)
    const bundleBytes = fs.readFileSync(bundleFile)

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      arrayBuffer: () =>
        Promise.resolve(
          bundleBytes.buffer.slice(
            bundleBytes.byteOffset,
            bundleBytes.byteOffset + bundleBytes.byteLength
          )
        ),
    })
    vi.stubGlobal('fetch', fetchMock)

    await run(['notebook', 'clone', '42', destinationPath])

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/notebooks/42/git-bundle'),
      expect.objectContaining({
        headers: { Authorization: 'Bearer fake-bearer' },
      })
    )

    expect(runGit(['branch', '--show-current'], destinationPath)).toBe('main')
    expect(runGit(['rev-list', '--count', 'HEAD'], destinationPath)).toBe('1')
    expect(
      runGit(
        ['rev-list', '--max-parents=0', '--count', 'HEAD'],
        destinationPath
      )
    ).toBe('1')

    const sourceTree = runGit(['rev-parse', 'HEAD^{tree}'], sourceRepoDir)
    const destinationTree = runGit(
      ['rev-parse', 'HEAD^{tree}'],
      destinationPath
    )
    expect(destinationTree).toBe(sourceTree)

    expect(
      runGit(['config', '--local', 'donut.notebook-id'], destinationPath)
    ).toBe('42')
    expect(
      runGit(['config', '--local', 'donut.api-origin'], destinationPath)
    ).toBe(getApiConfig().apiBaseUrl)

    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('publishing is not available')
    )
  })

  test('clone checks out "main" even when the machine defaults to a different branch name', async () => {
    const bundleFile = join(workDir, 'notebook.bundle')
    runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], sourceRepoDir)
    const bundleBytes = fs.readFileSync(bundleFile)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(
            bundleBytes.buffer.slice(
              bundleBytes.byteOffset,
              bundleBytes.byteOffset + bundleBytes.byteLength
            )
          ),
      })
    )

    const savedGitConfigCount = process.env.GIT_CONFIG_COUNT
    const savedGitConfigKey0 = process.env.GIT_CONFIG_KEY_0
    const savedGitConfigValue0 = process.env.GIT_CONFIG_VALUE_0
    process.env.GIT_CONFIG_COUNT = '1'
    process.env.GIT_CONFIG_KEY_0 = 'init.defaultBranch'
    process.env.GIT_CONFIG_VALUE_0 = 'totally-different-default'
    try {
      await run(['notebook', 'clone', '42', destinationPath])
    } finally {
      if (savedGitConfigCount === undefined) delete process.env.GIT_CONFIG_COUNT
      else process.env.GIT_CONFIG_COUNT = savedGitConfigCount
      if (savedGitConfigKey0 === undefined) delete process.env.GIT_CONFIG_KEY_0
      else process.env.GIT_CONFIG_KEY_0 = savedGitConfigKey0
      if (savedGitConfigValue0 === undefined)
        delete process.env.GIT_CONFIG_VALUE_0
      else process.env.GIT_CONFIG_VALUE_0 = savedGitConfigValue0
    }

    expect(runGit(['branch', '--show-current'], destinationPath)).toBe('main')
  })

  test('missing destination argument is rejected with the existing CLI error style', async () => {
    await expect(run(['notebook', 'clone', '42'])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('non-numeric notebook id is rejected with the existing CLI error style', async () => {
    await expect(
      run(['notebook', 'clone', 'not-a-number', destinationPath])
    ).rejects.toThrow(ProcessExitForTest)
    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })

  test('unknown notebook action is rejected with the existing CLI error style', async () => {
    await expect(
      run(['notebook', 'frobnicate', '42', destinationPath])
    ).rejects.toThrow(ProcessExitForTest)
    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })
})
