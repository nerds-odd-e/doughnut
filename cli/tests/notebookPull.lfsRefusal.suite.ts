import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import { checkoutState } from './notebookPull.testHelpers.js'

/** Interim LFS receive boundary: refuse pull before any local mutation. */
export function describeNotebookPullLfsRefusal(): void {
  describe('notebook pull (LFS interim refusal)', () => {
    const ctx = installNotebookCliRunFixture('donut-cli-pull-lfs-refusal-test-')
    let fetchMock: ReturnType<typeof vi.fn>

    beforeEach(() => {
      fetchMock = vi.fn()
      vi.stubGlobal('fetch', fetchMock)
    })

    afterEach(() => {
      vi.unstubAllGlobals()
    })

    test('LFS checkout refuses pull before changing refs or files', async () => {
      const directory = initBoundCheckout(
        ctx.getWorkDir(),
        getApiConfig().apiBaseUrl
      )
      fs.writeFileSync(
        join(directory, '.gitattributes'),
        '* filter=lfs diff=lfs merge=lfs -text\n'
      )
      runGit(['add', '.gitattributes'], directory)
      runGit(['commit', '--quiet', '-m', 'enable lfs'], directory)
      fs.writeFileSync(join(directory, 'unpublished.md'), '# keep me\n')
      const before = checkoutState(directory)
      const unpublishedBefore = fs.readFileSync(
        join(directory, 'unpublished.md'),
        'utf8'
      )

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          'Receiving into an existing LFS checkout is not supported yet'
        )
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('donut notebook clone')
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(fs.readFileSync(join(directory, 'unpublished.md'), 'utf8')).toBe(
        unpublishedBefore
      )
      expect(fetchMock).not.toHaveBeenCalled()
    })
  })
}
