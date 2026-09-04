import { describe, test, expect, vi } from 'vitest'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCloneCliTest,
  stagingDirsUnderTmp,
  expectNoNewStagingDirsSince,
} from './notebookClone.testHelpers.js'

describe('notebook clone (CLI routing, real Git checkout) — failed acquisition', () => {
  const ctx = installNotebookCloneCliTest()

  async function expectCloneFailure(before: string[]): Promise<void> {
    await expect(
      run(['notebook', 'clone', '42', ctx.getDestinationPath()])
    ).rejects.toThrow(ProcessExitForTest)
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    expectNoNewStagingDirsSince(before)
  }

  test('existing destination is refused through the existing CLI error style, leaving it untouched', async () => {
    const destinationPath = ctx.getDestinationPath()
    fs.mkdirSync(destinationPath)
    fs.writeFileSync(join(destinationPath, 'sentinel.txt'), 'pre-existing')
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const before = stagingDirsUnderTmp()

    await expectCloneFailure(before)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('donut: ')
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining(destinationPath)
    )
    expect(fs.readFileSync(join(destinationPath, 'sentinel.txt'), 'utf8')).toBe(
      'pre-existing'
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('missing git is reported through the existing CLI error style, leaving the destination untouched', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('irrelevant').buffer),
      })
    )
    const before = stagingDirsUnderTmp()
    const savedPath = process.env.PATH

    try {
      process.env.PATH = ''
      await expectCloneFailure(before)
    } finally {
      process.env.PATH = savedPath
    }

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringMatching(/^donut: .*git.*/i)
    )
    expect(fs.existsSync(ctx.getDestinationPath())).toBe(false)
  })

  test('an auth-denied download (403) is reported through the existing CLI error style, leaving the destination untouched', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 403 })
    vi.stubGlobal('fetch', fetchMock)
    const before = stagingDirsUnderTmp()

    await expectCloneFailure(before)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('donut: ')
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('does not have permission')
    )
    expect(fs.existsSync(ctx.getDestinationPath())).toBe(false)
  })

  test('a generic failed download (500) is reported through the existing CLI error style, leaving the destination untouched', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 500 })
    vi.stubGlobal('fetch', fetchMock)
    const before = stagingDirsUnderTmp()

    await expectCloneFailure(before)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('donut: ')
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('server returned an error')
    )
    expect(fs.existsSync(ctx.getDestinationPath())).toBe(false)
  })

  test('an invalid bundle fails real git clone and is reported through the existing CLI error style', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(
            new TextEncoder().encode('not a real git bundle').buffer
          ),
      })
    )
    const before = stagingDirsUnderTmp()

    await expectCloneFailure(before)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringMatching(/^donut: .*clone.*/i)
    )
    expect(fs.existsSync(ctx.getDestinationPath())).toBe(false)
  })
})
