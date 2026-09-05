import { describe, test, expect, vi, afterEach } from 'vitest'
import * as fs from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  runGit,
  installNotebookCliRunFixture,
} from './notebookClone.testHelpers.js'
import {
  initBoundCheckout,
  stubFetchWithBundleFile,
  stubFetchForSubmission,
  stubFetchWithAcceptedBundleFrom,
  buildSourceRepo,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'

function commitFileChange(
  dir: string,
  contents: string,
  message: string
): void {
  fs.writeFileSync(join(dir, 'note.md'), contents)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', message], dir)
}

// Notes are kept single-file (not split across sibling test files) because every publish-flow
// test — binding, readiness, ancestry, and submission alike — passes through
// `assertLocalMainFollowsAcceptedHistory`, which stages its own temp directory under this
// prefix. The ancestry leak-detection assertions below only hold because Vitest runs tests
// within one file serially; splitting into separate files lets Vitest schedule them onto
// concurrent workers, so another file's in-flight publish call can leave a same-prefixed
// directory visible during this file's before/after snapshot, producing a false leak failure.
function ancestryStagingDirsUnderTmp(): string[] {
  return fs
    .readdirSync(tmpdir())
    .filter((name) => name.startsWith('donut-notebook-publish-ancestry-'))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

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

describe('notebook publish (CLI routing, local readiness checks)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-readiness-test-')

  test('detached HEAD is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    const sha = runGit(['rev-parse', 'HEAD'], dir)
    runGit(['checkout', '--quiet', sha], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('detached HEAD')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('HEAD on a branch other than main is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    runGit(['checkout', '--quiet', '-b', 'feature'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('feature')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('a staged (index) change is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'note.md'), '# hello notebook (staged edit)\n')
    runGit(['add', 'note.md'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('an unstaged modification to a tracked file is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'note.md'), '# hello notebook (unstaged edit)\n')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('an untracked file is rejected with an actionable readiness error', async () => {
    const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
    fs.writeFileSync(join(dir, 'untracked.md'), '# new note\n')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('uncommitted changes')
    )
    expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
  })

  test('a clean checkout on main reaches the not-yet-available response', async () => {
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

describe('notebook publish (CLI routing, ancestry checks)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-ancestry-test-')

  test('local main identical to the accepted head passes through to the not-yet-available response', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'checkout'
    )

    const before = ancestryStagingDirsUnderTmp()
    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
    expect(ancestryStagingDirsUnderTmp()).toEqual(before)
  })

  test('local main exactly one direct commit ahead of the accepted head passes through to the not-yet-available response', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'checkout'
    )
    commitFileChange(dir, '# hello notebook (edited)\n', 'edit note')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
  })

  test('local main stale (behind the accepted head) is rejected with an ancestry error', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)

    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'checkout'
    )

    // Accepted history advances past what local main descends from.
    commitFileChange(sourceRepoDir, '# hello notebook (v2)\n', 'second commit')
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const before = ancestryStagingDirsUnderTmp()
    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('single direct commit')
    )
    expect(ancestryStagingDirsUnderTmp()).toEqual(before)
  })

  test('local main several commits ahead of the accepted head is rejected with an ancestry error', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'checkout'
    )
    commitFileChange(dir, '# hello notebook (edit 1)\n', 'edit note 1')
    commitFileChange(dir, '# hello notebook (edit 2)\n', 'edit note 2')

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('single direct commit')
    )
  })

  test('local main with unrelated history is rejected with an ancestry error', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const dir = initBoundCheckout(workDir, getApiConfig().apiBaseUrl)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('single direct commit')
    )
  })

  test('local main tip is a merge commit whose one parent is the accepted head, rejected with an ancestry error', async () => {
    const workDir = ctx.getWorkDir()
    const sourceRepoDir = buildSourceRepo(workDir)
    const bundleFile = join(workDir, 'accepted.bundle')
    bundleMain(sourceRepoDir, bundleFile)
    stubFetchWithBundleFile(bundleFile)

    const dir = cloneAsBoundCheckout(
      workDir,
      sourceRepoDir,
      getApiConfig().apiBaseUrl,
      'checkout'
    )
    runGit(['checkout', '--quiet', '-b', 'feature'], dir)
    commitFileChange(dir, '# hello notebook (feature)\n', 'feature commit')
    runGit(['checkout', '--quiet', 'main'], dir)
    runGit(
      ['merge', '--no-ff', '--quiet', '-m', 'merge feature', 'feature'],
      dir
    )

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('single direct commit')
    )
  })
})

describe('notebook publish (CLI routing, submission transport contract)', () => {
  const ctx = installNotebookCliRunFixture('donut-cli-publish-submission-test-')

  // Builds an eligible bound checkout (clean, on main, ancestry-valid — identical to accepted
  // head) and stubs fetch to serve `bundleFile` for the ancestry GET and `postResponse` for the
  // submission POST. Returns the checkout dir and the fetch mock for call-args assertions.
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
    expect(ctx.getErrorSpy()).not.toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
    expect(runGit(['rev-parse', 'main'], dir)).toBe(headBefore)
    expect(runGit(['status', '--porcelain'], dir)).toBe(statusBefore)
  })

  test('a non-2xx, non-40x submission response (today\'s real "not implemented" response) falls through to the existing generic message', async () => {
    const workDir = ctx.getWorkDir()
    const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
      status: 501,
      ok: false,
      text: () => Promise.resolve('Publishing is not available yet.'),
    })

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )
    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringContaining('not available yet')
    )
  })

  test('a 200 submission response with a stubbed accepted head proves the CLI success-rendering contract', async () => {
    const workDir = ctx.getWorkDir()
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
    try {
      const { dir } = setUpEligibleCheckoutWithPostResponse(workDir, {
        status: 200,
        ok: true,
        text: () => Promise.resolve('deadbeefcafef00ddeadbeefcafef00ddeadbeef'),
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
    const { dir, fetchMock } = setUpEligibleCheckoutWithPostResponse(workDir, {
      status: 501,
      ok: false,
      text: () => Promise.resolve(''),
    })
    const localMain = runGit(['rev-parse', 'main'], dir)

    await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
      ProcessExitForTest
    )

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

    // Confirm the posted bytes are a real git bundle of local main by cloning them.
    const postedBundleFile = join(workDir, 'posted.bundle')
    fs.writeFileSync(postedBundleFile, init.body)
    const clonedDir = join(workDir, 'posted-clone')
    runGit(['clone', '--quiet', postedBundleFile, clonedDir], workDir)
    expect(runGit(['rev-parse', 'main'], clonedDir)).toBe(localMain)
  })
})
