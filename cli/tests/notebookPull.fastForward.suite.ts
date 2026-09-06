import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
} from './notebookPull.testHelpers.js'

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

    test('receives an added note with its exact bytes and no Portable metadata', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const addedPath = 'Added note.md'
      const addedBytes = Buffer.from(
        '---\ntype: Note\nauthored: retained\n---\n# Added note\n\nExact body.\n'
      )
      fs.writeFileSync(join(source, addedPath), addedBytes)
      runGit(['add', addedPath], source)
      runGit(['commit', '--quiet', '-m', 'accepted note addition'], source)
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      const bundleFile = join(ctx.getWorkDir(), 'accepted-addition.bundle')
      bundleMain(source, bundleFile)
      ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
      expect(fs.readFileSync(join(directory, addedPath))).toEqual(addedBytes)
      expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
      expect(
        runGit(['ls-tree', '-r', '--name-only', 'HEAD'], directory).split('\n')
      ).toEqual([addedPath, 'note.md'])
    })

    test.each([
      {
        change: 'staged work',
        error: 'commit or clean them before receiving',
        mutate(directory: string) {
          fs.writeFileSync(join(directory, 'note.md'), '# staged local edit\n')
          runGit(['add', 'note.md'], directory)
        },
      },
      {
        change: 'unstaged work',
        error: 'commit or clean them before receiving',
        mutate(directory: string) {
          fs.writeFileSync(
            join(directory, 'note.md'),
            '# unstaged local edit\n'
          )
        },
      },
      {
        change: 'an untracked file',
        error: 'commit or clean them before receiving',
        mutate(directory: string) {
          fs.writeFileSync(join(directory, 'local.md'), '# local work\n')
        },
      },
      {
        change: 'a new HEAD',
        error: 'Local main changed while the accepted history was downloading',
        mutate(directory: string) {
          runGit(
            ['commit', '--quiet', '--allow-empty', '-m', 'concurrent commit'],
            directory
          )
        },
      },
      {
        change: 'another branch',
        error: 'Switch to main before receiving',
        mutate(directory: string) {
          runGit(['checkout', '--quiet', '-b', 'local-work'], directory)
        },
      },
    ])(
      'refuses without overwriting $change created while the download is completing',
      async ({ error, mutate }) => {
        const source = buildSourceRepo(ctx.getWorkDir())
        const directory = cloneAsBoundCheckout(
          ctx.getWorkDir(),
          source,
          getApiConfig().apiBaseUrl,
          'checkout'
        )
        fs.writeFileSync(join(source, 'note.md'), '# accepted remote edit\n')
        runGit(['add', 'note.md'], source)
        runGit(['commit', '--quiet', '-m', 'accepted remote edit'], source)
        const bundleFile = join(ctx.getWorkDir(), 'accepted-ahead.bundle')
        bundleMain(source, bundleFile)
        const response = bundleGetResponse(bundleFile)
        let stateAfterConcurrentChange: ReturnType<typeof checkoutState>
        ctx.getFetchMock().mockResolvedValue({
          ...response,
          arrayBuffer: async () => {
            mutate(directory)
            stateAfterConcurrentChange = checkoutState(directory)
            return response.arrayBuffer()
          },
        })
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(checkoutState(directory)).toEqual(stateAfterConcurrentChange!)
        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          expect.stringContaining(error)
        )
        expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
