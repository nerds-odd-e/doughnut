import * as fs from 'node:fs'
import { join } from 'node:path'
import { vi } from 'vitest'
import { runGit } from './notebookClone.testHelpers.js'
import {
  bindNotebookCheckout,
  configureTestGitIdentity,
  initGitRepoWithInitialNote,
} from './notebookGit.testHelpers.js'

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

function successfulPostResponse(): {
  status: number
  ok: boolean
  text: () => Promise<string>
} {
  return {
    status: 200,
    ok: true,
    text: () => Promise.resolve('deadbeefcafef00ddeadbeefcafef00ddeadbeef'),
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
// accept the POST (proposal submission). Eligibility-check tests use this helper to prove the
// request reaches submission; rejection behavior belongs to the submission-transport suite.
export function stubFetchWithBundleFile(
  bundleFile: string
): ReturnType<typeof vi.fn> {
  return stubFetchForSubmission(bundleFile, successfulPostResponse())
}

// Stubs global fetch to serve an accepted bundle built from `dir`'s own current state, so
// `dir`'s local main is identical to the accepted head — the ancestry check's pass-through case.
export function stubFetchWithAcceptedBundleFrom(
  dir: string,
  workDir: string
): ReturnType<typeof vi.fn> {
  const bundleFile = join(
    workDir,
    `accepted-${Date.now()}-${Math.random()}.bundle`
  )
  runGit(['bundle', 'create', bundleFile, 'HEAD', 'main'], dir)
  return stubFetchWithBundleFile(bundleFile)
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
