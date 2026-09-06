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

function checkoutState(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    branch: runGit(['rev-parse', '--abbrev-ref', 'HEAD'], directory),
    branches: runGit(
      ['for-each-ref', '--format=%(refname) %(objectname)', 'refs/heads'],
      directory
    ),
    status: runGit(['status', '--porcelain=v1'], directory),
    staged: runGit(['diff', '--cached'], directory),
    unstaged: runGit(['diff'], directory),
  }
}

describe('notebook pull (CLI routing and local readiness)', () => {
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

  test('detached HEAD is rejected without moving HEAD or branches', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    runGit(['checkout', '--quiet', '--detach'], directory)
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('Switch to main before receiving')
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('a branch other than main is rejected without losing its unpublished commit', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    runGit(['checkout', '--quiet', '-b', 'local-work'], directory)
    fs.writeFileSync(join(directory, 'local.md'), '# unpublished work\n')
    runGit(['add', 'local.md'], directory)
    runGit(['commit', '--quiet', '-m', 'local unpublished work'], directory)
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('Switch to main before receiving')
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(fs.readFileSync(join(directory, 'local.md'), 'utf8')).toBe(
      '# unpublished work\n'
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('a staged change is rejected without changing the index', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    fs.writeFileSync(join(directory, 'note.md'), '# staged local edit\n')
    runGit(['add', 'note.md'], directory)
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('commit or clean them before receiving')
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('an unstaged change is rejected without changing the working tree', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    fs.writeFileSync(join(directory, 'note.md'), '# unstaged local edit\n')
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('commit or clean them before receiving')
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('an untracked file is rejected without deleting it', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    fs.writeFileSync(
      join(directory, 'untracked.md'),
      '# untracked local work\n'
    )
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('commit or clean them before receiving')
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(fs.readFileSync(join(directory, 'untracked.md'), 'utf8')).toBe(
      '# untracked local work\n'
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('an active merge is rejected even when porcelain status is clean', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    runGit(['checkout', '--quiet', '-b', 'other-history'], directory)
    runGit(
      ['commit', '--quiet', '--allow-empty', '-m', 'other branch commit'],
      directory
    )
    runGit(['checkout', '--quiet', 'main'], directory)
    runGit(
      ['commit', '--quiet', '--allow-empty', '-m', 'local main commit'],
      directory
    )
    runGit(['merge', '--no-commit', 'other-history'], directory)
    expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
    const mergeHeadBefore = runGit(['rev-parse', 'MERGE_HEAD'], directory)
    const before = checkoutState(directory)

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining(
        'Finish or abort the active Git operation before receiving'
      )
    )
    expect(checkoutState(directory)).toEqual(before)
    expect(runGit(['rev-parse', 'MERGE_HEAD'], directory)).toBe(mergeHeadBefore)
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
