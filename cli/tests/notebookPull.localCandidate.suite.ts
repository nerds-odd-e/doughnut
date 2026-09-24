import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  commitFileChangeSet,
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import { prepareUnsupportedLocalHistory } from './notebookPull.localCandidate.testHelpers.js'

export function describeNotebookPullLocalCandidate(): void {
  describe('notebook pull (local candidate)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-local-candidate-test-'
    )

    test.each([
      {
        shape: 'unrelated',
        message:
          'Local main cannot receive the accepted history because it does not share Git history with the accepted notebook. Clone the notebook with "donut notebook clone", then try again.',
      },
      {
        shape: 'merge',
        message:
          'Local main cannot receive the accepted history because an unpublished commit is a merge. Recreate the local work as ordinary commits, then try again.',
      },
    ] as const)(
      'explains unsupported $shape local history without changing the checkout',
      async ({ shape, message }) => {
        const source = buildSourceRepo(ctx.getWorkDir())
        const directory = prepareUnsupportedLocalHistory(
          ctx.getWorkDir(),
          source,
          shape
        )
        serveAcceptedBundle(ctx, source, shape)
        const before = checkoutState(directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${message}`)
        expect(checkoutState(directory)).toEqual(before)
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )

    test('rebases local commits that add, change, rename and delete notes and files', async () => {
      const workDir = ctx.getWorkDir()
      const source = buildSourceRepo(workDir)
      commitFileChangeSet(
        source,
        [
          { path: 'other.md', content: '# Other\n' },
          { path: 'gone.md', content: '# Gone\n' },
        ],
        'shared base'
      )
      const directory = cloneAsBoundCheckout(
        workDir,
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      commitFileChangeSet(
        directory,
        [
          { path: 'Added.md', content: '# Added\n' },
          { path: 'data.bin', content: '\u0000first' },
        ],
        'add note and file'
      )
      commitPortableFile(directory, 'data.bin', '\u0000second', 'change file')
      runGit(['mv', 'note.md', 'Renamed.md'], directory)
      runGit(['rm', '--quiet', 'gone.md'], directory)
      runGit(['commit', '--quiet', '-m', 'rename and delete notes'], directory)
      commitPortableFile(source, 'other.md', '# Other\n\nWeb.\n', 'web save')
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      serveAcceptedBundle(ctx, source, 'local-shapes')

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD~3'], directory)).toBe(acceptedHead)
      expect(runGit(['ls-files'], directory).split('\n')).toEqual([
        'Added.md',
        'Renamed.md',
        'data.bin',
        'other.md',
      ])
      expect(fs.readFileSync(join(directory, 'data.bin'), 'utf8')).toBe(
        '\u0000second'
      )
      expect(fs.readFileSync(join(directory, 'other.md'), 'utf8')).toBe(
        '# Other\n\nWeb.\n'
      )
    })
  })
}
