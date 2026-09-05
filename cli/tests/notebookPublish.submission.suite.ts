import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

export function describeNotebookPublishSubmission(): void {
  describe('notebook publish (CLI routing, submission transport contract)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-submission-test-'
    )

    function setUpEligibleCheckoutWithPostResponse(
      workDir: string,
      postResponse: { status: number; ok: boolean; text: () => Promise<string> }
    ): { dir: string; fetchMock: ReturnType<typeof vi.fn> } {
      const sourceRepoDir = buildSourceRepo(workDir)
      const bundleFile = join(workDir, 'accepted.bundle')
      bundleMain(sourceRepoDir, bundleFile)
      const fetchMock = stubFetchForSubmission(bundleFile, postResponse)

      const dir = cloneAsBoundCheckout(
        workDir,
        sourceRepoDir,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      return { dir, fetchMock }
    }

    test('a 401/403 submission response is a distinct permission-denied error, leaving local state untouched', async () => {
      const workDir = ctx.getWorkDir()
      const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
        status: 403,
        ok: false,
        text: () => Promise.resolve(''),
      })
      const headBefore = runGit(['rev-parse', 'main'], dir)
      const statusBefore = runGit(['status', '--porcelain'], dir)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining("don't have permission to publish")
      )
      expect(runGit(['rev-parse', 'main'], dir)).toBe(headBefore)
      expect(runGit(['status', '--porcelain'], dir)).toBe(statusBefore)
    })

    test.each([
      [400, 'note.md has invalid YAML frontmatter', 'BINDING_ERROR'],
      [
        409,
        "expectedHead no longer matches the notebook's current accepted head.",
        'RESOURCE_CONFLICT',
      ],
    ])(
      'a %i ApiError reports the publication rejection reason and leaves local state untouched',
      async (status, message, errorType) => {
        const workDir = ctx.getWorkDir()
        const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
          status,
          ok: false,
          text: () => Promise.resolve(JSON.stringify({ message, errorType })),
        })
        const headBefore = runGit(['rev-parse', 'main'], dir)
        const statusBefore = runGit(['status', '--porcelain'], dir)

        await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
          ProcessExitForTest
        )
        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${message}`)
        expect(runGit(['rev-parse', 'main'], dir)).toBe(headBefore)
        expect(runGit(['status', '--porcelain'], dir)).toBe(statusBefore)
      }
    )

    test('a rejection without an ApiError message reports its HTTP status', async () => {
      const workDir = ctx.getWorkDir()
      const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
        status: 502,
        ok: false,
        text: () => Promise.resolve('<html>upstream failure</html>'),
      })

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        'donut: notebook publication rejected (HTTP 502)'
      )
    })

    test('a 200 submission response with a stubbed accepted head proves the CLI success-rendering contract', async () => {
      const workDir = ctx.getWorkDir()
      const logSpy = vi
        .spyOn(console, 'log')
        .mockImplementation(() => undefined)
      try {
        const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
          status: 200,
          ok: true,
          text: () =>
            Promise.resolve('deadbeefcafef00ddeadbeefcafef00ddeadbeef'),
        })

        await run(['notebook', 'publish', dir])

        expect(logSpy).toHaveBeenCalledWith(
          expect.stringContaining('deadbeefcafef00ddeadbeefcafef00ddeadbeef')
        )
      } finally {
        logSpy.mockRestore()
      }
    })

    test('the POST sends the expected URL, headers, and a real bundle of local main', async () => {
      const workDir = ctx.getWorkDir()
      const { dir, fetchMock } = setUpEligibleCheckoutWithPostResponse(
        workDir,
        {
          status: 200,
          ok: true,
          text: () =>
            Promise.resolve('deadbeefcafef00ddeadbeefcafef00ddeadbeef'),
        }
      )
      const localMain = runGit(['rev-parse', 'main'], dir)

      await run(['notebook', 'publish', dir])

      const postCall = fetchMock.mock.calls.find(
        ([, init]: [unknown, { method?: string } | undefined]) =>
          init?.method === 'POST'
      )
      expect(postCall).toBeDefined()
      const [url, init] = postCall as [
        string,
        {
          method: string
          headers: Record<string, string>
          body: Buffer
        },
      ]
      expect(url).toContain('/notebooks/42/git-bundle?expectedHead=')
      expect(init.headers.Authorization).toBe('Bearer fake-bearer')
      expect(init.headers['Content-Type']).toBe('application/x-git-bundle')
      expect(Buffer.isBuffer(init.body)).toBe(true)
      expect(init.body.length).toBeGreaterThan(0)

      const postedBundleFile = join(workDir, 'posted.bundle')
      fs.writeFileSync(postedBundleFile, init.body)
      const clonedDir = join(workDir, 'posted-clone')
      runGit(
        [
          '-c',
          'init.defaultBranch=ci-default',
          'clone',
          '--quiet',
          '--branch',
          'main',
          postedBundleFile,
          clonedDir,
        ],
        workDir
      )
      expect(runGit(['rev-parse', 'main'], clonedDir)).toBe(localMain)
    })
  })
}
