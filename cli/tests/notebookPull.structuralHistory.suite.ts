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
          commitPortableFile(
            source,
            'other.md',
            '---\ntype: Note\n---\n# Other\n\nRecreated.\n',
            'accepted recreate'
          )
        },
        path: 'other.md',
      },
      {
        shape: 'readme',
        seed: (source: string) => {
          commitPortableFile(
            source,
            'README.md',
            '---\ntype: Readme\n---\n# Notebook\n',
            'add notebook readme'
          )
        },
        apply: (source: string) => {
          commitPortableFile(
            source,
            'README.md',
            '---\ntype: Readme\n---\n# Notebook\n\nAccepted readme.\n',
            'accepted readme'
          )
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
      {
        shape: 'same-path-reversed-rename',
        apply: (source: string) => {
          runGit(['mv', 'note.md', 'Renamed.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted rename away'], source)
          runGit(['mv', 'Renamed.md', 'note.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted rename back'], source)
        },
        path: 'Renamed.md',
      },
      {
        shape: 'new-folder',
        apply: (source: string) => {
          commitPortableFile(
            source,
            'NewFolder/added.md',
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
            'accepted nested addition'
          )
        },
        path: 'NewFolder/added.md',
      },
    ] as const)(
      'names the structural path for remote $shape and leaves the checkout unchanged',
      async ({ shape, apply, path, ...rest }) => {
        const { directory, source } = cloneWithLocalNoteAndRemoteOther(
          ctx.getWorkDir(),
          'seed' in rest ? rest.seed : undefined
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

    // These two shapes compose only compatible per-edge operations (an addition at an
    // already-represented root plus a save of a different pre-existing note; two root
    // additions in one commit) so they are received, not refused.
    test.each([
      {
        shape: 'addition-with-edit',
        apply: (source: string) => {
          fs.writeFileSync(
            join(source, 'added.md'),
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n'
          )
          fs.writeFileSync(
            join(source, 'other.md'),
            '---\ntype: Note\n---\n# Other\n\nAccepted edit too.\n'
          )
          runGit(['add', 'added.md', 'other.md'], source)
          runGit(
            ['commit', '--quiet', '-m', 'accepted addition and edit'],
            source
          )
        },
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
          'other.md': '---\ntype: Note\n---\n# Other\n\nAccepted edit too.\n',
        },
      },
      {
        shape: 'two-additions',
        apply: (source: string) => {
          fs.writeFileSync(
            join(source, 'added.md'),
            '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n'
          )
          fs.writeFileSync(
            join(source, 'also.md'),
            '---\ntype: Note\n---\n# Also\n\nSecond addition.\n'
          )
          runGit(['add', 'added.md', 'also.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted two additions'], source)
        },
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
          'also.md': '---\ntype: Note\n---\n# Also\n\nSecond addition.\n',
        },
      },
    ] as const)(
      'receives compatible remote $shape',
      async ({ apply, expectedFiles, shape }) => {
        const { directory, source } = cloneWithLocalNoteAndRemoteOther(
          ctx.getWorkDir()
        )
        apply(source)
        const acceptedHead = runGit(['rev-parse', 'main'], source)
        serveAcceptedBundle(ctx, source, `compatible-${shape}`)

        await run(['notebook', 'pull', directory])

        expect(runGit(['rev-parse', 'HEAD^'], directory)).toBe(acceptedHead)
        for (const [path, content] of Object.entries(expectedFiles)) {
          expect(fs.readFileSync(join(directory, path), 'utf8')).toBe(content)
        }
      }
    )
  })
}
