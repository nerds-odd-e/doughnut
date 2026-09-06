import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'

describe('notebook pull (CLI routing and binding checks)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-pull-test-')
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  test('missing directory reports pull usage without making a request', async () => {
    await expect(run(['notebook', 'pull'])).rejects.toThrow(ProcessExitForTest)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      'donut: usage: donut notebook pull <directory>'
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('unbound directory explains the clone prerequisite without changing it', async () => {
    const directory = join(ctx.getWorkDir(), 'plain')
    fs.mkdirSync(directory)
    fs.writeFileSync(join(directory, 'keep.txt'), 'keep me')
    const entriesBefore = fs.readdirSync(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not a Donut notebook checkout')
    )
    expect(fs.readdirSync(directory)).toEqual(entriesBefore)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('checkout from another API origin is rejected without changing Git state', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      'https://other-donut.example.com'
    )
    const headBefore = runGit(['rev-parse', 'HEAD'], directory)
    const statusBefore = runGit(['status', '--porcelain=v1'], directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('other-donut.example.com')
    )
    expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(headBefore)
    expect(runGit(['status', '--porcelain=v1'], directory)).toBe(statusBefore)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('valid binding reports that receive is unavailable without claiming success', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      'donut: Receiving accepted notebook history is not available yet.'
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
