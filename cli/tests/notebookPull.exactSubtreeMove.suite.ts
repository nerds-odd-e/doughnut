import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  FOLDER_README,
  OUTSIDE_NOTE,
  PASTA_LOCAL,
  PASTA_ORIGINAL,
  SAUCE_BYTES,
  commitExactFolderMove,
  headTreeBlobMap,
  listHeadTree,
  prepareExactSubtreeMoveWithLocalDescendantEdit,
} from './notebookPull.exactSubtreeMove.testHelpers.js'

export function describeNotebookPullExactSubtreeMove(): void {
  describe('notebook pull (exact accepted subtree move with local descendant edit)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-exact-subtree-move-test-'
    )

    test('replays one local descendant edit onto an accepted Recipes→Kitchen/Recipes move', async () => {
      const setup = prepareExactSubtreeMoveWithLocalDescendantEdit(
        ctx.getWorkDir(),
        {
          localEditPath: 'Recipes/Pasta.md',
          localBytes: PASTA_LOCAL,
          move: { from: 'Recipes', to: 'Kitchen/Recipes' },
        }
      )
      serveAcceptedBundle(ctx, setup.source, 'exact-move-canonical')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()
      const originalAuthor = runGit(
        ['log', '-1', '--format=%an <%ae>', setup.localTip],
        setup.directory
      )
      const originalMessage = runGit(
        ['log', '-1', '--format=%B', setup.localTip],
        setup.directory
      )
      const acceptedBlobs = headTreeBlobMap(setup.source)

      await run(['notebook', 'pull', setup.directory])

      const localHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', setup.acceptedHead], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(listHeadTree(setup.directory)).toBe(
        [
          'Kitchen/README.md',
          'Kitchen/Recipes/Pasta.md',
          'Kitchen/Recipes/README.md',
          'note.md',
          'outside.md',
        ].join('\n')
      )
      expect(
        fs.readFileSync(
          join(setup.directory, 'Kitchen/Recipes/Pasta.md'),
          'utf8'
        )
      ).toBe(PASTA_LOCAL)
      expect(
        fs.readFileSync(join(setup.directory, 'Kitchen/README.md'), 'utf8')
      ).toBe(FOLDER_README.replace('# Folder', '# Kitchen'))
      expect(
        fs.readFileSync(
          join(setup.directory, 'Kitchen/Recipes/README.md'),
          'utf8'
        )
      ).toBe(FOLDER_README.replace('# Folder', '# Recipes'))
      expect(fs.readFileSync(join(setup.directory, 'outside.md'), 'utf8')).toBe(
        OUTSIDE_NOTE
      )
      const resultBlobs = headTreeBlobMap(setup.directory)
      for (const [path, blob] of acceptedBlobs) {
        if (path === 'Kitchen/Recipes/Pasta.md') continue
        expect(resultBlobs.get(path)).toBe(blob)
      }
      expect(runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)).toBe(
        originalAuthor
      )
      expect(runGit(['log', '-1', '--format=%B'], setup.directory)).toBe(
        originalMessage
      )
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock().mock.calls[0]?.[0]).toContain('/git-bundle')
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased onto the accepted history. Unpublished local commit: ${localHead}. Accepted head: ${setup.acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test.each([
      {
        label: 'nested descendant under Recipes→Kitchen/Recipes',
        bundleName: 'exact-move-nested',
        seed: (source: string) => {
          commitPortableFile(
            source,
            'Recipes/Sauces/README.md',
            FOLDER_README.replace('# Folder', '# Sauces'),
            'add Sauces'
          )
          commitPortableFile(
            source,
            'Recipes/Sauces/Pesto.md',
            SAUCE_BYTES,
            'add Pesto'
          )
        },
        localEditPath: 'Recipes/Sauces/Pesto.md',
        localBytes: '---\ntype: Note\n---\n# Pasta\n\nPesto local.\n',
        move: { from: 'Recipes', to: 'Kitchen/Recipes' },
        mappedPath: 'Kitchen/Recipes/Sauces/Pesto.md',
      },
      {
        label: 'descendant edit when Recipes moves to root',
        bundleName: 'exact-move-to-root',
        seed: (source: string) => {
          commitExactFolderMove(source, 'Recipes', 'Kitchen/Recipes')
        },
        localEditPath: 'Kitchen/Recipes/Pasta.md',
        localBytes: PASTA_LOCAL,
        move: { from: 'Kitchen/Recipes', to: 'Recipes' },
        mappedPath: 'Recipes/Pasta.md',
      },
      {
        label: 'identical-content sibling distinguished by relative path',
        bundleName: 'exact-move-identical-twin',
        seed: (source: string) => {
          commitPortableFile(
            source,
            'Recipes/Twin.md',
            PASTA_ORIGINAL,
            'add identical Twin'
          )
        },
        localEditPath: 'Recipes/Pasta.md',
        localBytes: PASTA_LOCAL,
        move: { from: 'Recipes', to: 'Kitchen/Recipes' },
        mappedPath: 'Kitchen/Recipes/Pasta.md',
        twinPath: 'Kitchen/Recipes/Twin.md',
        twinBytes: PASTA_ORIGINAL,
      },
    ] as const)(
      'maps $label',
      async ({
        seed,
        bundleName,
        localEditPath,
        localBytes,
        move,
        mappedPath,
        ...rest
      }) => {
        const setup = prepareExactSubtreeMoveWithLocalDescendantEdit(
          ctx.getWorkDir(),
          { localEditPath, localBytes, move, seed }
        )
        serveAcceptedBundle(ctx, setup.source, bundleName)

        await run(['notebook', 'pull', setup.directory])

        expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
          setup.acceptedHead
        )
        expect(fs.readFileSync(join(setup.directory, mappedPath), 'utf8')).toBe(
          localBytes
        )
        if ('twinPath' in rest && rest.twinPath !== undefined) {
          expect(
            fs.readFileSync(join(setup.directory, rest.twinPath), 'utf8')
          ).toBe(rest.twinBytes)
        }
      }
    )
  })
}
