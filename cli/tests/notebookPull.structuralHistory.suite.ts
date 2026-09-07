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
  installNotebookPullAcceptedHistoryTest,
} from './notebookPull.testHelpers.js'
import { serveAcceptedBundle } from './notebookPull.historySafety.testHelpers.js'

export function describeNotebookPullStructuralHistory(): void {
  describe('notebook pull (divergent structural accepted history)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-structural-history-test-'
    )

    test.each([
      {
        shape: 'rename',
        apply: (source: string) => {
          runGit(['mv', 'other.md', 'Renamed.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted rename'], source)
        },
        path: 'Renamed.md',
      },
      {
        shape: 'delete-recreate',
        apply: (source: string) => {
          runGit(['rm', '--quiet', 'other.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted delete'], source)
          fs.writeFileSync(
            join(source, 'other.md'),
            '---\ntype: Note\n---\n# Other\n\nRecreated.\n'
          )
          runGit(['add', 'other.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted recreate'], source)
        },
        path: 'other.md',
      },
      {
        shape: 'readme',
        apply: (source: string) => {
          fs.writeFileSync(
            join(source, 'README.md'),
            '---\ntype: Readme\n---\n# Notebook\n\nAccepted readme.\n'
          )
          runGit(['add', 'README.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted readme'], source)
        },
        path: 'README.md',
      },
      {
        shape: 'mode',
        apply: (source: string) => {
          fs.chmodSync(join(source, 'other.md'), 0o755)
          runGit(['add', 'other.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted mode'], source)
        },
        path: 'other.md',
      },
      {
        shape: 'reversed-rename',
        apply: (source: string) => {
          runGit(['mv', 'other.md', 'Renamed.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted rename away'], source)
          runGit(['mv', 'Renamed.md', 'other.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted rename back'], source)
        },
        path: 'Renamed.md',
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
          `donut: Local main cannot receive the accepted history because accepted history includes a structural change at "${path}". Divergent structural history is not supported yet.`
        )
        expect(checkoutState(directory)).toEqual(before)
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}

function cloneWithLocalNoteAndRemoteOther(workDir: string): {
  directory: string
  source: string
} {
  const source = buildSourceRepo(workDir)
  fs.writeFileSync(
    join(source, 'other.md'),
    '---\ntype: Note\n---\n# Other\n\nAccepted body.\n'
  )
  runGit(['add', 'other.md'], source)
  runGit(['commit', '--quiet', '-m', 'add other note'], source)
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  fs.writeFileSync(
    join(directory, 'note.md'),
    '---\ntype: Note\n---\n# Note\n\nLocal body.\n'
  )
  runGit(['add', 'note.md'], directory)
  runGit(['commit', '--quiet', '-m', 'unpublished note edit'], directory)
  return { directory, source }
}
