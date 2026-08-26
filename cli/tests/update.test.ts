import { describe, test, expect, vi, beforeEach } from 'vitest'
import * as childProcess from 'node:child_process'
import { formatVersionOutput, getVersion } from '../src/commands/version.js'

function newerThan(version: string): string {
  return `${Number(version.split('.')[0]) + 1}.0.0`
}

vi.mock('node:child_process', () => ({
  spawnSync: vi.fn(),
}))

vi.mock('node:fs', async (importOriginal) => {
  const fs = await importOriginal<typeof import('node:fs')>()
  return {
    ...fs,
    renameSync: vi.fn(),
    chmodSync: vi.fn(),
  }
})

beforeEach(() => {
  process.argv[1] = '/path/to/donut'
})

describe('runUpdate', () => {
  test('reports already latest when incoming version equals current', async () => {
    const consoleSpy = vi
      .spyOn(console, 'log')
      .mockImplementation(() => undefined)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () => Promise.resolve(new ArrayBuffer(0)),
      })
    )

    const currentBanner = formatVersionOutput()
    vi.mocked(childProcess.spawnSync).mockReturnValue({
      stdout: currentBanner,
      stderr: '',
      status: 0,
      error: undefined,
    } as ReturnType<typeof childProcess.spawnSync>)

    process.env.BASE_URL = 'http://localhost:9081'

    const { runUpdate } = await import('../src/commands/update.js')
    await runUpdate()

    expect(consoleSpy).toHaveBeenCalledWith(
      `${currentBanner} is already the latest version`
    )
  })

  test('reports Updated donut from … to … when incoming version is newer (install E2E)', async () => {
    const consoleSpy = vi
      .spyOn(console, 'log')
      .mockImplementation(() => undefined)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () => Promise.resolve(new ArrayBuffer(0)),
      })
    )

    const current = getVersion()
    const newer = newerThan(current)
    vi.mocked(childProcess.spawnSync).mockReturnValue({
      stdout: `donut ${newer}`,
      stderr: '',
      status: 0,
      error: undefined,
    } as ReturnType<typeof childProcess.spawnSync>)

    process.env.BASE_URL = 'http://localhost:9081'

    const { runUpdate } = await import('../src/commands/update.js')
    await runUpdate()

    expect(consoleSpy).toHaveBeenCalledWith(
      `Updated donut from ${current} to ${newer}`
    )
  })

  test('reports error when download returns non-ok status', async () => {
    const consoleSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)
    const exitSpy = vi.spyOn(process, 'exit').mockImplementation(((
      code?: number
    ) => {
      throw new Error(`exit ${code}`)
    }) as typeof process.exit)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 404 })
    )

    process.env.BASE_URL = 'http://localhost:9081'

    const { runUpdate } = await import('../src/commands/update.js')
    await expect(runUpdate()).rejects.toThrow('exit')

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringMatching(/donut: download failed: HTTP 404/)
    )
    expect(exitSpy).toHaveBeenCalledWith(1)
  })
})
