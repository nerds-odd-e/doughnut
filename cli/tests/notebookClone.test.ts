import { describe, test, expect, vi } from 'vitest'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  runGit,
  installNotebookCloneCliTest,
} from './notebookClone.testHelpers.js'

describe('notebook clone (CLI routing, real Git checkout)', () => {
  const ctx = installNotebookCloneCliTest()

  test('clone downloads the accepted bundle and checks it out as a clean single-commit main branch', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = ctx.getSourceRepoDir()
    const destinationPath = ctx.getDestinationPath()

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

    expect(ctx.getLogSpy()).toHaveBeenCalledWith(
      expect.stringContaining('publishing is not available')
    )
  })

  test('clone checks out "main" even when the machine defaults to a different branch name', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = ctx.getSourceRepoDir()
    const destinationPath = ctx.getDestinationPath()

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
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('non-numeric notebook id is rejected with the existing CLI error style', async () => {
    await expect(
      run(['notebook', 'clone', 'not-a-number', ctx.getDestinationPath()])
    ).rejects.toThrow(ProcessExitForTest)
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('unknown notebook action is rejected with the existing CLI error style', async () => {
    await expect(
      run(['notebook', 'frobnicate', '42', ctx.getDestinationPath()])
    ).rejects.toThrow(ProcessExitForTest)
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('usage: donut notebook clone')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })
})
