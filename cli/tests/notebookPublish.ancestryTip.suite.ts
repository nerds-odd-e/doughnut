import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
  commitFileChange,
  postCount,
  stubFetchForSubmission,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'

/**
 * Publish ancestry edges for already-accepted tips and non-contiguous local history.
 */
export function describeNotebookPublishAncestryTipEdges(): void {
  describe('notebook publish (accepted tip and non-contiguous ancestry)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-ancestry-tip-test-'
    )

    test('retrying when a multi-commit tip is already accepted reports that tip and leaves local chain intact', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)

      const dir = cloneAsBoundCheckout(
        workDir,
        sourceRepoDir,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const firstEdit = '# hello notebook (edit 1)\n'
      const secondEdit = '# hello notebook (edit 2)\n'
      commitFileChange(dir, firstEdit, 'edit note 1')
      const middle = runGit(['rev-parse', 'main'], dir)
      commitFileChange(dir, secondEdit, 'edit note 2')
      const tip = runGit(['rev-parse', 'main'], dir)
      const acceptedAtA = runGit(['rev-parse', 'main^^'], dir)

      const alreadyAcceptedBundle = join(workDir, 'already-accepted-tip.bundle')
      bundleMain(dir, alreadyAcceptedBundle)
      const fetchMock = stubFetchForSubmission(alreadyAcceptedBundle, {
        status: 200,
        ok: true,
        text: () => Promise.resolve(tip),
      })
      const logSpy = vi
        .spyOn(console, 'log')
        .mockImplementation(() => undefined)
      try {
        await run(['notebook', 'publish', dir])

        expect(logSpy).toHaveBeenCalledWith(
          expect.stringContaining(`Published notebook. Accepted head: ${tip}`)
        )
        expect(logSpy).toHaveBeenCalledWith(
          expect.stringContaining(
            'acquire a fresh checkout elsewhere with "donut notebook clone'
          )
        )
        expect(postCount(fetchMock)).toBe(1)
        const postCall = fetchMock.mock.calls.find(
          ([, init]: [unknown, { method?: string } | undefined]) =>
            init?.method === 'POST'
        )
        expect(postCall?.[0]).toContain(
          `/notebooks/42/git-bundle?expectedHead=${encodeURIComponent(tip)}`
        )
        expect(runGit(['rev-parse', 'main'], dir)).toBe(tip)
        expect(runGit(['rev-parse', 'main^'], dir)).toBe(middle)
        expect(runGit(['rev-parse', 'main^^'], dir)).toBe(acceptedAtA)
        expect(fs.readFileSync(join(dir, 'note.md'), 'utf8')).toBe(secondEdit)
      } finally {
        logSpy.mockRestore()
      }
    })

    test('local main with unrelated history is rejected with an ancestry error', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)
      const bundleFile = join(workDir, 'accepted.bundle')
      bundleMain(sourceRepoDir, bundleFile)
      stubFetchWithBundleFile(bundleFile)

      const dir = initBoundCheckout(workDir, getApiConfig().apiBaseUrl)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('contiguous single-parent commit range')
      )
    })

    test('local main tip is a merge commit whose one parent is the accepted head, rejected with an ancestry error', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)
      const bundleFile = join(workDir, 'accepted.bundle')
      bundleMain(sourceRepoDir, bundleFile)
      stubFetchWithBundleFile(bundleFile)

      const dir = cloneAsBoundCheckout(
        workDir,
        sourceRepoDir,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      runGit(['checkout', '--quiet', '-b', 'feature'], dir)
      commitFileChange(dir, '# hello notebook (feature)\n', 'feature commit')
      runGit(['checkout', '--quiet', 'main'], dir)
      runGit(
        ['merge', '--no-ff', '--quiet', '-m', 'merge feature', 'feature'],
        dir
      )

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('contiguous single-parent commit range')
      )
    })
  })
}
