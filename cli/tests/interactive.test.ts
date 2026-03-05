import { describe, test, expect, beforeAll, vi } from 'vitest'
import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { processInput } from '../src/interactive.js'

const cliDir = fileURLToPath(new URL('../', import.meta.url))
const bundlePath = join(cliDir, 'dist', 'doughnut-cli.bundle.mjs')

function runCli(input: string): {
  stdout: string
  stderr: string
  status: number
} {
  const result = spawnSync('node', [bundlePath], {
    input,
    encoding: 'utf8',
    timeout: 5000,
    env: { ...process.env, CLI_VERSION: '0.1.0' },
  })
  return {
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? '',
    status: result.status ?? -1,
  }
}

describe('processInput', () => {
  test('returns true for exit', () => {
    expect(processInput('exit')).toBe(true)
    expect(processInput('  exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(processInput('')).toBe(false)
    expect(processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
    logSpy.mockRestore()
  })

  test('returns false and logs "Not supported" for any other input', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    expect(processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    logSpy.mockRestore()
  })
})

describe('interactive CLI (e2e style)', () => {
  beforeAll(() => {
    if (!existsSync(bundlePath)) {
      throw new Error(
        `CLI bundle not found at ${bundlePath}. Run 'pnpm cli:bundle' first.`
      )
    }
  })

  test('responds "Not supported" to any input', () => {
    const { stdout } = runCli('hello\nexit')
    expect(stdout).toContain('Not supported')
  })

  test('exit command exits the CLI', () => {
    const { status } = runCli('exit')
    expect(status).toBe(0)
  })

  test('shift-enter adds newline without triggering response (piped mode processes each line)', () => {
    const { stdout } = runCli('line1\nline2\nexit')
    expect(stdout).toContain('Not supported')
    const notSupportedCount = (stdout.match(/Not supported/g) ?? []).length
    expect(notSupportedCount).toBe(2)
  })

  test('shows Cursor-like UI with version, box, and placeholder', () => {
    const { stdout } = runCli('exit')
    expect(stdout).toContain('doughnut 0.1.0')
    expect(stdout).toContain('→')
    expect(stdout).toContain('Plan, search, build anything')
    expect(stdout).toContain('exit to quit')
  })
})
