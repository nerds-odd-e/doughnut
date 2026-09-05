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

// Sets the test-only Git identity a fresh local repo needs before it can commit.
function configureTestGitIdentity(dir: string): void {
  runGit(['config', 'user.email', 'test@example.com'], dir)
  runGit(['config', 'user.name', 'Test'], dir)
}

// Initializes a fresh repo on `main` with one committed note.md — the common starting state
// shared by every checkout and source repo these tests build. `label` is folded into the note
// content so two independently-created repos never coincidentally commit identical content
// (which, combined with an identical author/committer timestamp, would produce the same SHA and
// make an "unrelated history" checkout indistinguishable from the accepted head it should differ
// from).
function initGitRepoWithInitialNote(dir: string, label: string): void {
  fs.mkdirSync(dir)
  runGit(['init', '--quiet', '-b', 'main'], dir)
  configureTestGitIdentity(dir)
  fs.writeFileSync(join(dir, 'note.md'), `# hello notebook (${label})\n`)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', 'initial notebook commit'], dir)
}

// Records the local-only Git config binding that `notebook clone` would have recorded.
function bindNotebookCheckout(dir: string, apiOrigin: string): void {
  runGit(['config', '--local', 'donut.notebook-id', '42'], dir)
  runGit(['config', '--local', 'donut.api-origin', apiOrigin], dir)
}

// Produces a bound checkout that is also clean and committed on `main` — the
// baseline eligible state for the readiness checks added in this slice.
function initBoundCheckout(workDir: string, apiOrigin: string): string {
  const dir = join(workDir, 'checkout')
  initGitRepoWithInitialNote(dir, 'checkout')
  bindNotebookCheckout(dir, apiOrigin)
  return dir
}

// Stubs global fetch to serve the accepted bundle at `bundleFile`.
function stubFetchWithBundleFile(bundleFile: string): void {
  const bundleBytes = fs.readFileSync(bundleFile)
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: true,
      arrayBuffer: () =>
        Promise.resolve(
          bundleBytes.buffer.slice(
            bundleBytes.byteOffset,
            bundleBytes.byteOffset + bundleBytes.byteLength
          )
        ),
    })
  )
}

// Stubs global fetch to serve an accepted bundle built from `dir`'s own current state, so
// `dir`'s local main is identical to the accepted head — the ancestry check's pass-through case.
function stubFetchWithAcceptedBundleFrom(dir: string, workDir: string): void {
  const bundleFile = join(
    workDir,
    `accepted-${Date.now()}-${Math.random()}.bundle`
  )
  runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], dir)
  stubFetchWithBundleFile(bundleFile)
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

// A real Git source repo used to build the "accepted" bundle served by the mocked fetch, so
// the ancestry checks below run against real Git plumbing rather than fakes.
function buildSourceRepo(workDir: string, name = 'source'): string {
  const dir = join(workDir, name)
  initGitRepoWithInitialNote(dir, name)
  return dir
}

function bundleMain(sourceRepoDir: string, bundleFile: string): void {
  runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], sourceRepoDir)
}

// Clones `sourceRepoDir` so the checkout's main head is an identical commit object to the
// accepted bundle built from that same source state, then binds it like `notebook clone` would.
function cloneAsBoundCheckout(
  workDir: string,
  sourceRepoDir: string,
  apiOrigin: string,
  name: string
): string {
  const dir = join(workDir, name)
  runGit(['clone', '--quiet', sourceRepoDir, dir], workDir)
  bindNotebookCheckout(dir, apiOrigin)
  configureTestGitIdentity(dir)
  runGit(['remote', 'remove', 'origin'], dir)
  return dir
}

function commitFileChange(
  dir: string,
  contents: string,
  message: string
): void {
  fs.writeFileSync(join(dir, 'note.md'), contents)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', message], dir)
}

function ancestryStagingDirsUnderTmp(): string[] {
  return fs
    .readdirSync(tmpdir())
    .filter((name) => name.startsWith('donut-notebook-publish-ancestry-'))
}

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
