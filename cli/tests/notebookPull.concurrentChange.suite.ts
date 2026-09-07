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
import { prepareEligibleDivergence } from './notebookPull.rebase.testHelpers.js'

function mergeHeadPath(directory: string) {
  return join(
    directory,
    runGit(['rev-parse', '--git-path', 'MERGE_HEAD'], directory)
  )
}

function concurrentCheckoutState(directory: string) {
  const file = mergeHeadPath(directory)
  return {
    ...checkoutState(directory),
    mergeHead: fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '',
  }
}

const concurrentChanges = [
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
      fs.writeFileSync(join(directory, 'note.md'), '# unstaged local edit\n')
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
  {
    change: 'an active Git operation',
    error: 'Finish or abort the active Git operation before receiving',
    mutate(directory: string) {
      fs.writeFileSync(
        mergeHeadPath(directory),
        `${runGit(['rev-parse', 'HEAD'], directory)}\n`
      )
    },
  },
] as const

function prepareFastForwardAhead(workDir: string): {
  directory: string
  source: string
} {
  const source = buildSourceRepo(workDir)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  fs.writeFileSync(join(source, 'note.md'), '# accepted remote edit\n')
  runGit(['add', 'note.md'], source)
  runGit(['commit', '--quiet', '-m', 'accepted remote edit'], source)
  return { directory, source }
}

const histories = [
  {
    history: 'fast-forward accepted history',
    prepare: prepareFastForwardAhead,
  },
  {
    history: 'eligible other-note divergence',
    prepare(workDir: string) {
      const setup = prepareEligibleDivergence(workDir, { remoteEdits: 1 })
      return { directory: setup.directory, source: setup.source }
    },
  },
] as const

export function describeNotebookPullConcurrentChange(): void {
  describe('notebook pull (concurrent checkout during download)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-concurrent-change-test-'
    )

    test.each(
      histories.flatMap((history) =>
        concurrentChanges.map((change) => ({ ...history, ...change }))
      )
    )(
      'refuses without overwriting $change created while $history downloads',
      async ({ error, mutate, prepare }) => {
        const { directory, source } = prepare(ctx.getWorkDir())
        const bundleFile = join(ctx.getWorkDir(), 'accepted.bundle')
        bundleMain(source, bundleFile)
        const response = bundleGetResponse(bundleFile)
        let stateAfterConcurrentChange: ReturnType<
          typeof concurrentCheckoutState
        >
        ctx.getFetchMock().mockResolvedValue({
          ...response,
          arrayBuffer: async () => {
            mutate(directory)
            stateAfterConcurrentChange = concurrentCheckoutState(directory)
            return response.arrayBuffer()
          },
        })
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(concurrentCheckoutState(directory)).toEqual(
          stateAfterConcurrentChange!
        )
        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          expect.stringContaining(error)
        )
        expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
