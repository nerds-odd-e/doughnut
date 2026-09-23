import * as fs from 'node:fs'
import { dirname, join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
  commitFileChange,
  postCount,
  stubFetchWithBundleFile,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'

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
  })
}
