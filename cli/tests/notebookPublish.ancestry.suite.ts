import * as fs from 'node:fs'
import { dirname, join } from 'node:path'
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
  postCount,
  stubFetchForSubmission,
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

function commitRelatedNoteChanges(
  dir: string,
  files: { relativePath: string; contents: string }[],
  message: string
): void {
  for (const file of files) {
    const absolutePath = join(dir, file.relativePath)
    fs.mkdirSync(dirname(absolutePath), { recursive: true })
    fs.writeFileSync(absolutePath, file.contents)
    runGit(['add', file.relativePath], dir)
  }
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
        expect.stringContaining('contiguous single-parent commit range')
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(before)
    })

    test('a two-note edit commit stale after a later accepted web save is rejected without posting, leaving local head and both files intact', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)
      commitRelatedNoteChanges(
        sourceRepoDir,
        [
          {
            relativePath: 'Nested/Cell.md',
            contents: '---\ntype: Note\n---\n# Nested\n\nOriginal.\n',
          },
        ],
        'add nested note'
      )

      const dir = cloneAsBoundCheckout(
        workDir,
        sourceRepoDir,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const rootEdited = '# hello notebook (local root edit)\n'
      const nestedEdited =
        '---\ntype: Note\nauthored: local-yaml\n---\n# Nested\n\nLocal nested body.\n'
      commitRelatedNoteChanges(
        dir,
        [
          { relativePath: 'note.md', contents: rootEdited },
          { relativePath: 'Nested/Cell.md', contents: nestedEdited },
        ],
        'edit related notes'
      )
      const localHead = runGit(['rev-parse', 'main'], dir)

      commitFileChange(
        sourceRepoDir,
        '# hello notebook (web save)\n',
        'later web content save'
      )
      const bundleFile = join(workDir, 'accepted.bundle')
      bundleMain(sourceRepoDir, bundleFile)
      const fetchMock = stubFetchWithBundleFile(bundleFile)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('contiguous single-parent commit range')
      )
      expect(postCount(fetchMock)).toBe(0)
      expect(runGit(['rev-parse', 'main'], dir)).toBe(localHead)
      expect(fs.readFileSync(join(dir, 'note.md'), 'utf8')).toBe(rootEdited)
      expect(fs.readFileSync(join(dir, 'Nested', 'Cell.md'), 'utf8')).toBe(
        nestedEdited
      )
    })

    test('local main several content-edit commits ahead of the accepted head reaches submission', async () => {
      const workDir = ctx.getWorkDir()
      const sourceRepoDir = buildSourceRepo(workDir)
      runGit(['checkout', '--quiet', '-b', 'feature'], sourceRepoDir)
      commitFileChange(
        sourceRepoDir,
        '# hello notebook (side)\n',
        'side commit'
      )
      runGit(['checkout', '--quiet', 'main'], sourceRepoDir)
      runGit(
        [
          'merge',
          '--no-ff',
          '--quiet',
          '-m',
          'merge below accepted',
          'feature',
        ],
        sourceRepoDir
      )
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
      const localHead = runGit(['rev-parse', 'main'], dir)

      await run(['notebook', 'publish', dir])
      expect(runGit(['rev-parse', 'main'], dir)).toBe(localHead)
    })

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
          `Published notebook. Accepted head: ${tip}`
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
