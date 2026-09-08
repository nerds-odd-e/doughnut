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
  STRUCTURAL_CHANGE_REFUSAL,
} from './notebookPull.testHelpers.js'
import {
  PASTA_LOCAL,
  commitExactFolderMove,
  prepareExactSubtreeMoveWithLocalDescendantEdit,
  seedRecipesKitchenTree,
} from './notebookPull.exactSubtreeMove.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { getApiConfig } from 'donut-api'

export function describeNotebookPullExactSubtreeMoveRefusal(): void {
  describe('notebook pull (exact subtree move refusals)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-exact-subtree-move-refusal-test-'
    )

    test('refuses a local edit outside the moved subtree without changing the checkout', async () => {
      const setup = prepareExactSubtreeMoveWithLocalDescendantEdit(
        ctx.getWorkDir(),
        {
          localEditPath: 'outside.md',
          localBytes:
            '---\ntype: Note\n---\n# Outside\n\nLocal outside edit.\n',
          move: { from: 'Recipes', to: 'Kitchen/Recipes' },
        }
      )
      serveAcceptedBundle(ctx, setup.source, 'exact-move-outside-edit')
      const before = checkoutState(setup.directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringMatching(STRUCTURAL_CHANGE_REFUSAL)
      )
      expect(checkoutState(setup.directory)).toEqual(before)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test.each([
      {
        shape: 'extra-remote-edit',
        apply: (source: string) => {
          commitExactFolderMove(source, 'Recipes', 'Kitchen/Recipes')
          commitPortableFile(
            source,
            'outside.md',
            '---\ntype: Note\n---\n# Outside\n\nAlso edited remotely.\n',
            'accepted extra edit'
          )
        },
      },
      {
        shape: 'changed-subtree-bytes',
        apply: (source: string) => {
          runGit(['mv', 'Recipes', 'Kitchen/Recipes'], source)
          fs.writeFileSync(
            join(source, 'Kitchen/Recipes/Pasta.md'),
            '---\ntype: Note\n---\n# Pasta\n\nRemote changed pasta.\n'
          )
          runGit(['add', 'Kitchen/Recipes'], source)
          runGit(
            ['commit', '--quiet', '-m', 'move with changed pasta bytes'],
            source
          )
        },
      },
      {
        shape: 'folder-rename',
        apply: (source: string) => {
          runGit(['mv', 'Recipes', 'Kitchen/Cookbooks'], source)
          runGit(
            ['commit', '--quiet', '-m', 'rename Recipes while moving'],
            source
          )
        },
      },
    ] as const)(
      'refuses unsupported remote $shape before checkout mutation',
      async ({ shape, apply }) => {
        const workDir = ctx.getWorkDir()
        const source = buildSourceRepo(workDir)
        seedRecipesKitchenTree(source)
        const directory = cloneAsBoundCheckout(
          workDir,
          source,
          getApiConfig().apiBaseUrl,
          'checkout'
        )
        commitPortableFile(
          directory,
          'Recipes/Pasta.md',
          PASTA_LOCAL,
          'unpublished descendant edit'
        )
        apply(source)
        serveAcceptedBundle(ctx, source, `exact-move-refuse-${shape}`)
        const before = checkoutState(directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          expect.stringMatching(STRUCTURAL_CHANGE_REFUSAL)
        )
        expect(checkoutState(directory)).toEqual(before)
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
