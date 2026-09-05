import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  initBoundCheckout,
  stubFetchWithAcceptedBundleFrom,
} from './notebookPublish.testHelpers.js'

export function describeNotebookPublishReadiness(): void {
  describe('notebook publish (CLI routing, local readiness checks)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-readiness-test-'
    )

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
      fs.writeFileSync(
        join(dir, 'note.md'),
        '# hello notebook (unstaged edit)\n'
      )

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

    test('a clean checkout on main reaches submission', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      const fetchMock = stubFetchWithAcceptedBundleFrom(dir, ctx.getWorkDir())

      await run(['notebook', 'publish', dir])
      expect(fetchMock).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ method: 'POST' })
      )
    })
  })
}
