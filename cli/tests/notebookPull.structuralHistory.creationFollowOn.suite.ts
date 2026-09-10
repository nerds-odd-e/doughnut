import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import { cloneWithLocalNoteAndRemoteOther } from './notebookPull.structuralHistory.testHelpers.js'

const LOCAL_BODY = '---\ntype: Note\n---\n# Note\n\nLocal body.\n'

export function describeNotebookPullCreationFollowOnComposition(): void {
  describe('notebook pull (composed accepted addition follow-on)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-creation-follow-on-composition-test-'
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
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
          'also.md': '---\ntype: Note\n---\n# Also\n\nSecond addition.\n',
        },
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
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nAccepted addition.\n',
          'other.md': '---\ntype: Note\n---\n# Other\n\nAccepted other save.\n',
        },
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
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nAccepted save.\n',
          'other.md': '---\ntype: Note\n---\n# Other\n\nAccepted edit too.\n',
        },
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
        expectedFiles: {
          'added.md': '---\ntype: Note\n---\n# Added\n\nSecond save.\n',
        },
      },
    ] as const)(
      'receives compatible remote $shape while retaining the local edit',
      async ({ apply, expectedFiles, shape }) => {
        const { directory, source } = cloneWithLocalNoteAndRemoteOther(
          ctx.getWorkDir()
        )
        const localTip = runGit(['rev-parse', 'HEAD'], directory)
        apply(source)
        const acceptedHead = runGit(['rev-parse', 'main'], source)
        serveAcceptedBundle(ctx, source, `follow-on-${shape}`)

        await run(['notebook', 'pull', directory])

        expect(runGit(['rev-parse', 'HEAD^'], directory)).toBe(acceptedHead)
        expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
          LOCAL_BODY
        )
        for (const [path, content] of Object.entries(expectedFiles)) {
          expect(fs.readFileSync(join(directory, path), 'utf8')).toBe(content)
        }
        expect(runGit(['rev-parse', 'ORIG_HEAD'], directory)).toBe(localTip)
        expect(() =>
          runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
        ).not.toThrow()
      }
    )
  })
}
