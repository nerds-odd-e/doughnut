import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import { installNotebookPullAcceptedHistoryTest } from './notebookPull.testHelpers.js'

export function describeNotebookPullFastForward(): void {
  describe('notebook pull (accepted history fast-forward)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-fast-forward-test-'
    )

    test.each([1, 3])(
      'fast-forwards the same checkout across %i accepted commit(s)',
      async (acceptedCommitCount) => {
        const source = buildSourceRepo(ctx.getWorkDir())
        const directory = cloneAsBoundCheckout(
          ctx.getWorkDir(),
          source,
          getApiConfig().apiBaseUrl,
          'checkout'
        )
        const originalHead = runGit(['rev-parse', 'HEAD'], directory)
        const originalNotebookId = runGit(
          ['config', '--local', '--get', 'donut.notebook-id'],
          directory
        )
        const originalApiOrigin = runGit(
          ['config', '--local', '--get', 'donut.api-origin'],
          directory
        )

        for (let i = 1; i <= acceptedCommitCount; i += 1) {
          fs.writeFileSync(
            join(source, 'note.md'),
            `---\ntype: Note\nauthored: retained\n---\n# accepted edit ${i}\n`
          )
          runGit(['add', 'note.md'], source)
          runGit(['commit', '--quiet', '-m', `accepted edit ${i}`], source)
        }

        const acceptedHead = runGit(['rev-parse', 'main'], source)
        const acceptedTree = runGit(['rev-parse', 'main^{tree}'], source)
        const bundleFile = join(
          ctx.getWorkDir(),
          `accepted-${acceptedCommitCount}-ahead.bundle`
        )
        bundleMain(source, bundleFile)
        ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await run(['notebook', 'pull', directory])

        expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
        expect(runGit(['rev-parse', 'HEAD^{tree}'], directory)).toBe(
          acceptedTree
        )
        expect(
          runGit(['rev-list', '--count', `${originalHead}..HEAD`], directory)
        ).toBe(String(acceptedCommitCount))
        expect(() =>
          runGit(
            ['merge-base', '--is-ancestor', originalHead, 'HEAD'],
            directory
          )
        ).not.toThrow()
        expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
        expect(runGit(['for-each-ref', '--format=%(refname)'], directory)).toBe(
          'refs/heads/main'
        )
        expect(
          runGit(['config', '--local', '--get', 'donut.notebook-id'], directory)
        ).toBe(originalNotebookId)
        expect(
          runGit(['config', '--local', '--get', 'donut.api-origin'], directory)
        ).toBe(originalApiOrigin)
        expect(
          runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory)
        ).toBe('note.md')
        expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
        expect(ctx.getFetchMock()).toHaveBeenCalledWith(
          `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
          { headers: { Authorization: 'Bearer fake-bearer' } }
        )
        expect(ctx.getLogSpy()).toHaveBeenCalledWith(
          `Received accepted notebook history. Accepted head: ${acceptedHead}. This accepted history may not include all current web content.`
        )
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )

    test('receives every note change in one accepted commit with exact bytes and no Portable metadata', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const changedFiles = [
        {
          path: 'Added note.md',
          bytes: Buffer.from(
            '---\ntype: Note\nauthored: retained\n---\n# Added note\n\nExact body.\n'
          ),
        },
        {
          path: 'Related note.md',
          bytes: Buffer.from(
            '---\ntype: Note\n---\n# Related note\n\nRelated body.\n'
          ),
        },
        {
          path: 'note.md',
          bytes: Buffer.from(
            '---\ntype: Note\nauthored: retained\n---\n# Edited note\n\nUpdated body.\n'
          ),
        },
      ]
      for (const { path, bytes } of changedFiles) {
        fs.writeFileSync(join(source, path), bytes)
      }
      runGit(['add', ...changedFiles.map(({ path }) => path)], source)
      runGit(
        ['commit', '--quiet', '-m', 'accepted related note changes'],
        source
      )
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      const acceptedTree = runGit(['rev-parse', 'main^{tree}'], source)
      const bundleFile = join(ctx.getWorkDir(), 'accepted-note-changes.bundle')
      bundleMain(source, bundleFile)
      ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
      expect(runGit(['rev-parse', 'HEAD^{tree}'], directory)).toBe(acceptedTree)
      for (const { path, bytes } of changedFiles) {
        expect(fs.readFileSync(join(directory, path))).toEqual(bytes)
      }
      expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
      expect(
        runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory).split('\n')
      ).toEqual(changedFiles.map(({ path }) => path))
    })

    test('receives an accepted note deletion with exact remaining bytes and no Portable metadata', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const keptPath = 'kept.md'
      const keptBytes = Buffer.from(
        '---\ntype: Note\nauthored: retained\n---\n# Kept note\n\nUnchanged body.\n'
      )
      fs.writeFileSync(join(source, keptPath), keptBytes)
      runGit(['add', keptPath], source)
      runGit(['commit', '--quiet', '-m', 'add kept note'], source)
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const originalHead = runGit(['rev-parse', 'HEAD'], directory)
      runGit(['rm', '--', 'note.md'], source)
      runGit(['commit', '--quiet', '-m', 'delete note.md'], source)
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      const acceptedTree = runGit(['rev-parse', 'main^{tree}'], source)
      const bundleFile = join(ctx.getWorkDir(), 'accepted-deletion.bundle')
      bundleMain(source, bundleFile)
      ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))

      await run(['notebook', 'pull', directory])

      expect(fs.existsSync(join(directory, 'note.md'))).toBe(false)
      expect(fs.readFileSync(join(directory, keptPath))).toEqual(keptBytes)
      expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
      expect(runGit(['rev-parse', 'HEAD^{tree}'], directory)).toBe(acceptedTree)
      expect(() =>
        runGit(['merge-base', '--is-ancestor', originalHead, 'HEAD'], directory)
      ).not.toThrow()
      expect(runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory)).toBe(
        keptPath
      )
    })
  })
}
