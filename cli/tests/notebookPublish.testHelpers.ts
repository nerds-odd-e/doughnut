import * as fs from 'node:fs'
import { join } from 'node:path'
import { vi } from 'vitest'
import { runGit } from './notebookClone.testHelpers.js'

// Sets the test-only Git identity a fresh local repo needs before it can commit.
export function configureTestGitIdentity(dir: string): void {
  runGit(['config', 'user.email', 'test@example.com'], dir)
  runGit(['config', 'user.name', 'Test'], dir)
}

// Initializes a fresh repo on `main` with one committed note.md — the common starting state
// shared by every checkout and source repo these tests build. `label` is folded into the note
// content so two independently-created repos never coincidentally commit identical content
// (which, combined with an identical author/committer timestamp, would produce the same SHA and
// make an "unrelated history" checkout indistinguishable from the accepted head it should differ
// from).
export function initGitRepoWithInitialNote(dir: string, label: string): void {
  fs.mkdirSync(dir)
  runGit(['init', '--quiet', '-b', 'main'], dir)
  configureTestGitIdentity(dir)
  fs.writeFileSync(join(dir, 'note.md'), `# hello notebook (${label})\n`)
  runGit(['add', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', 'initial notebook commit'], dir)
}

// Records the local-only Git config binding that `notebook clone` would have recorded.
export function bindNotebookCheckout(dir: string, apiOrigin: string): void {
  runGit(['config', '--local', 'donut.notebook-id', '42'], dir)
  runGit(['config', '--local', 'donut.api-origin', apiOrigin], dir)
}

// Produces a bound checkout that is also clean and committed on `main` — the
// baseline eligible state for the readiness checks added in this slice.
export function initBoundCheckout(workDir: string, apiOrigin: string): string {
  const dir = join(workDir, 'checkout')
  initGitRepoWithInitialNote(dir, 'checkout')
  bindNotebookCheckout(dir, apiOrigin)
  return dir
}

// Builds a fetch response object serving the bundle bytes at `bundleFile`, as used for the GET
// accepted-bundle download (both the clone flow and the publish ancestry check).
function bundleGetResponse(bundleFile: string): {
  ok: boolean
  arrayBuffer: () => Promise<ArrayBuffer>
} {
  const bundleBytes = fs.readFileSync(bundleFile)
  return {
    ok: true,
    arrayBuffer: () =>
      Promise.resolve(
        bundleBytes.buffer.slice(
          bundleBytes.byteOffset,
          bundleBytes.byteOffset + bundleBytes.byteLength
        )
      ),
  }
}

// A generic non-2xx, non-40x submission response, standing in for today's real controller
// response (HTTP 501 "Publishing is not available yet.") for tests concerned only with the
// eligibility checks upstream of submission, not the submission response itself.
function interimRefusalPostResponse(): {
  status: number
  ok: boolean
  text: () => Promise<string>
} {
  return {
    status: 501,
    ok: false,
    text: () => Promise.resolve('Publishing is not available yet.'),
  }
}

/**
 * Stubs global fetch to distinguish the ancestry check's GET (accepted-bundle download) from the
 * submission's POST (proposal upload): GET requests are served `bundleFile`'s bytes; POST
 * requests get `postResponse`. Returns the mock so callers can assert on the captured POST call.
 */
export function stubFetchForSubmission(
  bundleFile: string,
  postResponse: { status: number; ok: boolean; text: () => Promise<string> }
): ReturnType<typeof vi.fn> {
  const fetchMock = vi.fn(
    (
      _url: unknown,
      init?: { method?: string }
    ): Promise<
      | { ok: boolean; arrayBuffer: () => Promise<ArrayBuffer> }
      | { status: number; ok: boolean; text: () => Promise<string> }
    > => {
      if (init?.method === 'POST') return Promise.resolve(postResponse)
      return Promise.resolve(bundleGetResponse(bundleFile))
    }
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

// Stubs global fetch to serve the accepted bundle at `bundleFile` for GET (bundle download) and
// today's real interim-refusal shape for POST (proposal submission) — the eligibility-check
// tests using this helper only care about reaching that generic response, not exercising its
// contract in detail (see the dedicated submission-transport test file for that).
export function stubFetchWithBundleFile(bundleFile: string): void {
  stubFetchForSubmission(bundleFile, interimRefusalPostResponse())
}

// Stubs global fetch to serve an accepted bundle built from `dir`'s own current state, so
// `dir`'s local main is identical to the accepted head — the ancestry check's pass-through case.
export function stubFetchWithAcceptedBundleFrom(
  dir: string,
  workDir: string
): void {
  const bundleFile = join(
    workDir,
    `accepted-${Date.now()}-${Math.random()}.bundle`
  )
  runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], dir)
  stubFetchWithBundleFile(bundleFile)
}

// A real Git source repo used to build the "accepted" bundle served by the mocked fetch, so
// the ancestry checks run against real Git plumbing rather than fakes.
export function buildSourceRepo(workDir: string, name = 'source'): string {
  const dir = join(workDir, name)
  initGitRepoWithInitialNote(dir, name)
  return dir
}

export function bundleMain(sourceRepoDir: string, bundleFile: string): void {
  runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], sourceRepoDir)
}

// Clones `sourceRepoDir` so the checkout's main head is an identical commit object to the
// accepted bundle built from that same source state, then binds it like `notebook clone` would.
export function cloneAsBoundCheckout(
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
