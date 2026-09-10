import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  abortPausedRebase,
  continuePausedRebaseWithChosenBytes,
} from './notebookPull.conflict.testHelpers.js'
import {
  commitFileChangeSet,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  ACCEPTED_THIRD_NOTE,
  LOCAL_NESTED_NOTE,
  LOCAL_ROOT_NOTE,
  prepareTwoNoteBatchDivergence,
} from './notebookPull.twoNoteBatch.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'

export function describeNotebookPullContentBatch(): void {
  describe('notebook pull (content batch over a linear accepted history)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-content-batch-test-'
    )

    const BATCH_SHAPES: {
      shape: string
      baseFiles: { path: string; content: string }[] | undefined
      acceptedChangeSets: { path: string; content: string }[][]
    }[] = [
      {
        shape: 'repeated-save',
        baseFiles: undefined,
        acceptedChangeSets: [
          [
            {
              path: 'gamma.md',
              content: '---\ntype: Note\n---\n# Gamma\n\nFirst accepted.\n',
            },
          ],
          [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
        ],
      },
      {
        shape: 'net-equivalent-interval',
        baseFiles: undefined,
        acceptedChangeSets: [
          [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
          [
            {
              path: 'gamma.md',
              content: '---\ntype: Note\n---\n# Gamma\n\nIntermediate.\n',
            },
          ],
          [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
        ],
      },
      {
        shape: 'multi-path-accepted-commit',
        baseFiles: [
          {
            path: 'Nested/Cell.md',
            content: '---\ntype: Note\n---\n# Nested\n\nOriginal nested.\n',
          },
          {
            path: 'gamma.md',
            content: '---\ntype: Note\n---\n# Gamma\n\nOriginal third.\n',
          },
          {
            path: 'delta.md',
            content: '---\ntype: Note\n---\n# Delta\n\nOriginal delta.\n',
          },
        ],
        acceptedChangeSets: [
          [
            { path: 'gamma.md', content: ACCEPTED_THIRD_NOTE },
            {
              path: 'delta.md',
              content: '---\ntype: Note\n---\n# Delta\n\nAccepted delta.\n',
            },
          ],
        ],
      },
    ]

    test.each(BATCH_SHAPES)(
      'retains a three-path local batch as one unpublished child over $shape',
      async ({ shape, baseFiles, acceptedChangeSets }) => {
        const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
          baseFiles,
          // Independent ordering: nested first, then root, then the redundant third path.
          localChanges: [
            { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
            { path: 'note.md', content: LOCAL_ROOT_NOTE },
            { path: 'gamma.md', content: ACCEPTED_THIRD_NOTE },
          ],
          acceptedChangeSets,
        })
        serveAcceptedBundle(ctx, setup.source, `content-batch-${shape}`)
        const originalAuthor = runGit(
          ['log', '-1', '--format=%an <%ae>', setup.localTip],
          setup.directory
        )
        const originalMessage = runGit(
          ['log', '-1', '--format=%s', setup.localTip],
          setup.directory
        )

        await run(['notebook', 'pull', setup.directory])

        expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
          setup.acceptedHead
        )
        expect(
          runGit(
            ['rev-list', '--count', 'HEAD', '--not', setup.acceptedHead],
            setup.directory
          )
        ).toBe('1')
        expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
          LOCAL_ROOT_NOTE
        )
        expect(
          fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
        ).toBe(LOCAL_NESTED_NOTE)
        expect(
          runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)
        ).toBe(originalAuthor)
        expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
          originalMessage
        )
        expect(() =>
          runGit(
            ['cat-file', '-e', `${setup.localTip}^{commit}`],
            setup.directory
          )
        ).not.toThrow()
      }
    )

    test('combines a non-overlapping same-file edit within a two-path local batch', async () => {
      const workDir = ctx.getWorkDir()
      const source = buildSourceRepo(workDir)
      commitFileChangeSet(
        source,
        [
          {
            path: 'note.md',
            content:
              '---\ntype: Note\n---\n# Note\n\nShared opening.\n\nShared closing.\n',
          },
          {
            path: 'Nested/Cell.md',
            content: '---\ntype: Note\n---\n# Nested\n\nOriginal nested.\n',
          },
        ],
        'add base files'
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
          {
            path: 'note.md',
            content:
              '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nShared closing.\n',
          },
          { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
        ],
        'unpublished batch'
      )
      const localTip = runGit(['rev-parse', 'HEAD'], directory)
      commitFileChangeSet(
        source,
        [
          {
            path: 'note.md',
            content:
              '---\ntype: Note\n---\n# Note\n\nShared opening.\n\nRemote closing.\n',
          },
        ],
        'accepted closing'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], source)
      serveAcceptedBundle(ctx, source, 'batch-non-overlap-combine')

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD^'], directory)).toBe(acceptedHead)
      expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Note\n\nLocal opening.\n\nRemote closing.\n'
      )
      expect(fs.readFileSync(join(directory, 'Nested/Cell.md'), 'utf8')).toBe(
        LOCAL_NESTED_NOTE
      )
      expect(() =>
        runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
      ).not.toThrow()
    })

    test('pauses on a two-file overlap and continues with an untouched companion edit intact', async () => {
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        localChanges: [
          { path: 'note.md', content: LOCAL_ROOT_NOTE },
          { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
          {
            path: 'gamma.md',
            content: '---\ntype: Note\n---\n# Gamma\n\nLocal companion edit.\n',
          },
        ],
        acceptedChangeSets: [
          [
            {
              path: 'note.md',
              content:
                '---\ntype: Note\n---\n# Alpha\n\nAccepted overlapping root.\n',
            },
            {
              path: 'Nested/Cell.md',
              content:
                '---\ntype: Note\n---\n# Nested\n\nAccepted overlapping nested.\n',
            },
          ],
        ],
      })
      serveAcceptedBundle(ctx, setup.source, 'batch-two-file-overlap')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(runGit(['ls-files', '-u'], setup.directory)).toContain('note.md')
      expect(runGit(['ls-files', '-u'], setup.directory)).toContain(
        'Nested/Cell.md'
      )
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Gamma\n\nLocal companion edit.\n'
      )

      continuePausedRebaseWithChosenBytes(setup.directory, [
        {
          path: 'note.md',
          content: '---\ntype: Note\n---\n# Alpha\n\nChosen root.\n',
        },
        {
          path: 'Nested/Cell.md',
          content: '---\ntype: Note\n---\n# Nested\n\nChosen nested.\n',
        },
      ])

      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Alpha\n\nChosen root.\n'
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe('---\ntype: Note\n---\n# Nested\n\nChosen nested.\n')
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Gamma\n\nLocal companion edit.\n'
      )
    })

    test('aborting a two-file overlap pause restores the entire original batch', async () => {
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        localChanges: [
          { path: 'note.md', content: LOCAL_ROOT_NOTE },
          { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
          {
            path: 'gamma.md',
            content: '---\ntype: Note\n---\n# Gamma\n\nLocal companion edit.\n',
          },
        ],
        acceptedChangeSets: [
          [
            {
              path: 'note.md',
              content:
                '---\ntype: Note\n---\n# Alpha\n\nAccepted overlapping root.\n',
            },
            {
              path: 'Nested/Cell.md',
              content:
                '---\ntype: Note\n---\n# Nested\n\nAccepted overlapping nested.\n',
            },
          ],
        ],
      })
      const originalTree = runGit(
        ['rev-parse', `${setup.localTip}^{tree}`],
        setup.directory
      )
      serveAcceptedBundle(ctx, setup.source, 'batch-two-file-overlap-abort')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      abortPausedRebase(setup.directory)

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(runGit(['write-tree'], setup.directory)).toBe(originalTree)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_ROOT_NOTE
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(LOCAL_NESTED_NOTE)
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Gamma\n\nLocal companion edit.\n'
      )
    })

    test('absorbs an LF-equivalent path while retaining a valuable companion edit', async () => {
      const withFinalLf =
        '---\ntype: Note\n---\n# Note\n\nSame authored body.\n'
      const withoutFinalLf =
        '---\ntype: Note\n---\n# Note\n\nSame authored body.'
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        baseFiles: [
          { path: 'note.md', content: '---\ntype: Note\n---\n' },
          {
            path: 'Nested/Cell.md',
            content: '---\ntype: Note\n---\n# Nested\n\nOriginal nested.\n',
          },
        ],
        localChanges: [
          { path: 'note.md', content: withFinalLf },
          { path: 'Nested/Cell.md', content: LOCAL_NESTED_NOTE },
        ],
        acceptedChangeSets: [[{ path: 'note.md', content: withoutFinalLf }]],
      })
      serveAcceptedBundle(ctx, setup.source, 'batch-lf-plus-valuable')

      await run(['notebook', 'pull', setup.directory])

      expect(
        runGit(
          ['rev-list', '--count', 'HEAD', '--not', setup.acceptedHead],
          setup.directory
        )
      ).toBe('1')
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        withoutFinalLf
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(LOCAL_NESTED_NOTE)
    })

    test('leaves no unpublished commit when the entire batch is LF-equivalent to accepted', async () => {
      const rootWithLf = '---\ntype: Note\n---\n# Note\n\nSame body.\n'
      const rootWithoutLf = '---\ntype: Note\n---\n# Note\n\nSame body.'
      const nestedWithLf =
        '---\ntype: Note\n---\n# Nested\n\nSame nested body.\n'
      const nestedWithoutLf =
        '---\ntype: Note\n---\n# Nested\n\nSame nested body.'
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        baseFiles: [
          { path: 'note.md', content: '---\ntype: Note\n---\n' },
          { path: 'Nested/Cell.md', content: '---\ntype: Note\n---\n' },
        ],
        localChanges: [
          { path: 'note.md', content: rootWithLf },
          { path: 'Nested/Cell.md', content: nestedWithLf },
        ],
        acceptedChangeSets: [
          [
            { path: 'note.md', content: rootWithoutLf },
            { path: 'Nested/Cell.md', content: nestedWithoutLf },
          ],
        ],
      })
      serveAcceptedBundle(ctx, setup.source, 'batch-fully-redundant')

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        rootWithoutLf
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(nestedWithoutLf)
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
    })
  })
}
