import { describe, test, expect } from 'vitest'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  runGit,
  installNotebookCliRunFixture,
} from './notebookClone.testHelpers.js'

// Produces a bound checkout that is also clean and committed on `main` — the
// baseline eligible state for the readiness checks added in this slice.
function initBoundCheckout(workDir: string, apiOrigin: string): string {
  const dir = join(workDir, 'checkout')
  fs.mkdirSync(dir)
  runGit(['init', '--quiet', '-b', 'main'], dir)
  runGit(['config', '--local', 'donut.notebook-id', '42'], dir)
  runGit(['config', '--local', 'donut.api-origin', apiOrigin], dir)
  runGit(['config', 'user.email', 'test@example.com'], dir)
  runGit(['config', 'user.name', 'Test'], dir)
  fs.writeFileSync(join(dir, 'note.md'), '# hello notebook\n')
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', 'initial notebook commit'], dir)
  return dir
}

describe('notebook publish (CLI routing, binding checks)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-test-')

  test('directory that is not a bound Donut checkout is rejected with a binding error', async () => {
    const dir = join(ctx.getWorkDir(), 'plain')
    fs.mkdirSync(dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not a Donut notebook checkout')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('directory bound to a different API origin is rejected with a binding error', async () => {
    const dir = initBoundCheckout(
      ctx.getWorkDir(),
      'https://other-donut.example.com'
    )

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('other-donut.example.com')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('directory bound to the currently configured API origin reaches the not-yet-available response', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })
})

describe('notebook publish (CLI routing, local readiness checks)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-readiness-test-')

  test('detached HEAD is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    const sha = runGit(['rev-parse', 'HEAD'], dir)
    runGit(['checkout', '--quiet', sha], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('detached HEAD')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('HEAD on a branch other than main is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    runGit(['checkout', '--quiet', '-b', 'feature'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('feature')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('a staged (index) change is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'note.md'), '# hello notebook (staged edit)\n')
    runGit(['add', 'note.md'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('an unstaged modification to a tracked file is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'note.md'), '# hello notebook (unstaged edit)\n')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('an untracked file is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'untracked.md'), '# new note\n')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('a clean checkout on main reaches the not-yet-available response', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })
})
