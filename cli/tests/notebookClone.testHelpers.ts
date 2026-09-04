import { execFileSync } from 'node:child_process'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, expect, vi } from 'vitest'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

export class ProcessExitForTest extends Error {
  readonly code: number | undefined
  constructor(code?: number) {
    super(`process.exit(${code})`)
    this.name = 'ProcessExitForTest'
    this.code = code
  }
}

export function runGit(args: string[], cwd: string): string {
  return execFileSync('git', args, { cwd, encoding: 'utf8' }).trim()
}

export function stagingDirsUnderTmp(): string[] {
  return fs
    .readdirSync(tmpdir())
    .filter((name) => name.startsWith('donut-notebook-clone-'))
}

/**
 * Asserts the run under test left no orphaned acquisition staging directory
 * behind. Compares one-directionally (only dirs present now but absent from
 * `before`) rather than snapshot equality: other CLI test files scan and
 * clean up the same shared os.tmpdir() prefix concurrently, so a directory
 * disappearing between the two snapshots reflects another file's timing, not
 * a leak from this run.
 */
export function expectNoNewStagingDirsSince(before: string[]): void {
  const after = stagingDirsUnderTmp()
  const leaked = after.filter((name) => !before.includes(name))
  expect(leaked).toEqual([])
}

/**
 * Shared fixture for "notebook clone (CLI routing, real Git checkout)" tests:
 * a real source Git repo to bundle from, a destination path that does not yet
 * exist, and the console/process spies the CLI's error path relies on.
 */
export function installNotebookCloneCliTest() {
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

    // Prefix deliberately does not start with the acquisition staging-dir
    // prefix ('donut-notebook-clone-', see notebookAcquisition.ts) so this
    // fixture's own scratch directory never shows up in stagingDirsUnderTmp()
    // — tests in this file run alongside other test files whose scratch dirs
    // can briefly coexist under the same os.tmpdir().
    workDir = fs.mkdtempSync(join(tmpdir(), 'donut-cli-clone-test-'))
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

  return {
    getWorkDir: () => workDir,
    getSourceRepoDir: () => sourceRepoDir,
    getDestinationPath: () => destinationPath,
    getErrorSpy: () => errorSpy,
    getLogSpy: () => logSpy,
    getExitSpy: () => exitSpy,
  }
}
