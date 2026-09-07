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

export function describeNotebookPullConcurrentChange(): void {
  describe('notebook pull (concurrent checkout during download)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-concurrent-change-test-'
    )

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
