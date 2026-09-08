import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
  structuralChangeRefusal,
} from './notebookPull.testHelpers.js'
import { cloneWithLocalNoteAndRemoteOther } from './notebookPull.structuralHistory.testHelpers.js'

export function describeNotebookPullCreationFollowOnRefusal(): void {
  describe('notebook pull (unsupported creation follow-on accepted history)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-creation-follow-on-refusal-test-'
    )

    test.each([
      {
        shape: 'creation-then-second-addition',
        apply: (source: string) => {
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
            'accepted addition'
          )
          commitPortableFile(
            source,
            'also.md',
            '---\ntype: Note\n---\n# Also\n\nSecond addition.\n',
            'accepted second addition'
          )
        },
        path: 'added.md',
      },
      {
        shape: 'creation-then-save-of-other',
        apply: (source: string) => {
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
            'accepted addition'
          )
          commitPortableFile(
            source,
            'other.md',
            '---\ntype: Note\n---\n# Other\n\nAccepted other save.\n',
            'accepted other save'
          )
        },
        path: 'added.md',
      },
      {
        shape: 'creation-then-save-with-other-edit',
        apply: (source: string) => {
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
            'accepted addition'
          )
          fs.writeFileSync(
            join(source, 'added.md'),
            '---\ntype: Note\n---\n# Added\n\nAccepted save.\n'
          )
          fs.writeFileSync(
            join(source, 'other.md'),
            '---\ntype: Note\n---\n# Other\n\nAccepted edit too.\n'
          )
          runGit(['add', 'added.md', 'other.md'], source)
          runGit(
            ['commit', '--quiet', '-m', 'accepted save with other edit'],
            source
          )
        },
        path: 'added.md',
      },
      {
        shape: 'creation-then-two-saves',
        apply: (source: string) => {
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
            'accepted addition'
          )
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted save.\n',
            'accepted save'
          )
          commitPortableFile(
            source,
            'added.md',
            '---\ntype: Note\n---\n# Added\n\nSecond save.\n',
            'accepted second save'
          )
        },
        path: 'added.md',
      },
    ] as const)(
      'names the structural path for remote $shape and leaves the checkout unchanged',
      async ({ shape, apply, path }) => {
        const { directory, source } = cloneWithLocalNoteAndRemoteOther(
          ctx.getWorkDir()
        )
        apply(source)
        serveAcceptedBundle(ctx, source, `structural-${shape}`)
        const before = checkoutState(directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          `donut: ${structuralChangeRefusal(path)}`
        )
        expect(checkoutState(directory)).toEqual(before)
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
