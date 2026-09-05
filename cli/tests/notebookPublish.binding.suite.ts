import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
} from './notebookClone.testHelpers.js'
import {
  initBoundCheckout,
  stubFetchWithAcceptedBundleFrom,
} from './notebookPublish.testHelpers.js'

export function describeNotebookPublishBinding(): void {
  describe('notebook publish (CLI routing, binding checks)', () => {
    const ctx = installNotebookCliRunFixture('donut-cli-publish-test-')

    test('directory that is not a bound Donut checkout is rejected with a binding error', async () => {
      const dir = join(ctx.getWorkDir(), 'plain')
      fs.mkdirSync(dir)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('not a Donut notebook checkout')
      )
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    })

    test('directory bound to a different API origin is rejected with a binding error', async () => {
      const dir = initBoundCheckout(
        ctx.getWorkDir(),
        'https://other-donut.example.com'
      )

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('other-donut.example.com')
      )
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    })

    test('directory bound to the currently configured API origin reaches the not-yet-available response', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      stubFetchWithAcceptedBundleFrom(dir, ctx.getWorkDir())

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('not available yet')
      )
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    })
  })
}
