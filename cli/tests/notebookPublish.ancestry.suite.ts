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
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'

function commitFileChange(
  dir: string,
  contents: string,
  message: string
): void {
  fs.writeFileSync(join(dir, 'note.md'), contents)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', message], dir)
}

export function describeNotebookPublishAncestry(): void {
  describe('notebook publish (CLI routing, ancestry checks)', () => {
    const ctx = installNotebookCliRunFixture('donut-cli-publish-ancestry-test-')

    test('local main identical to the accepted head reaches submission', async () => {
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

      const before = acceptedHistoryStagingDirsUnderTmp()
      await run(['notebook', 'publish', dir])
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(before)
    })

    test('local main exactly one direct commit ahead of the accepted head reaches submission', async () => {
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
      commitFileChange(dir, '# hello notebook (edited)\n', 'edit note')

      await run(['notebook', 'publish', dir])
    })

    test('local main stale (behind the accepted head) is rejected with an ancestry error', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)

      const dir = cloneAsBoundCheckout(
        workDir,
        sourceRepoDir,
        getApiConfig().apiBaseUrl,
        'checkout'
      )

      commitFileChange(
        sourceRepoDir,
        '# hello notebook (v2)\n',
        'second commit'
      )
      const bundleFile = join(workDir, 'accepted.bundle')
      bundleMain(sourceRepoDir, bundleFile)
      stubFetchWithBundleFile(bundleFile)

      const before = acceptedHistoryStagingDirsUnderTmp()
      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('single direct commit')
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(before)
    })

    test('local main several commits ahead of the accepted head is rejected with an ancestry error', async () => {
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
      commitFileChange(dir, '# hello notebook (edit 1)\n', 'edit note 1')
      commitFileChange(dir, '# hello notebook (edit 2)\n', 'edit note 2')

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('single direct commit')
      )
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
        expect.stringContaining('single direct commit')
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
        expect.stringContaining('single direct commit')
      )
    })
  })
}
