import { readFile } from 'node:fs/promises'
import { basename } from 'node:path'
import type { AttachBookRequestFull } from '../../../packages/generated/donut-backend-api/types.gen.js'
import {
  configureClient,
  getApiConfig,
  type BookFull,
  type RequestOptions,
} from 'donut-api'
import { loadStoredAccessToken } from './accessTokenStorage.js'
import {
  authenticatedBackendCallFailureAdvice,
  userVisibleMessageForSdkThrowable,
} from './authenticatedBackendCallFailure.js'

/**
 * For every call to the generated donut-backend-api HTTP client that runs inside
 * {@link withBackendClient} / {@link runWithDefaultBackendClient}: non-OK responses and
 * fetch failures throw instead of returning `{ error }`.
 */
export type DonutSdkCallOptions = Partial<Pick<RequestOptions, 'signal'>> & {
  throwOnError: true
}

export function donutSdkOptions(signal?: AbortSignal): DonutSdkCallOptions {
  return signal === undefined
    ? { throwOnError: true }
    : { throwOnError: true, signal }
}

function isFetchAbortedByCaller(error: unknown): boolean {
  return (
    (typeof DOMException !== 'undefined' &&
      error instanceof DOMException &&
      error.name === 'AbortError') ||
    (error instanceof Error && error.name === 'AbortError')
  )
}

export async function withBackendClient<T>(
  token: string,
  fn: () => Promise<T>
): Promise<T> {
  const { apiBaseUrl } = getApiConfig()
  configureClient(apiBaseUrl, token)
  try {
    return await fn()
  } catch (e) {
    if (isFetchAbortedByCaller(e)) throw e
    throw new Error(userVisibleMessageForSdkThrowable(e))
  }
}

export async function withBackendJson<T>(
  bearerToken: string,
  fn: () => Promise<unknown>
): Promise<T> {
  const envelope = await withBackendClient(bearerToken, fn)
  return (envelope as { data: T }).data
}

export async function runWithDefaultBackendClient<T>(
  fn: () => Promise<T>
): Promise<T> {
  const stored = loadStoredAccessToken()
  if (!stored) {
    throw new Error(authenticatedBackendCallFailureAdvice.noAccessTokenInConfig)
  }
  return withBackendClient(stored.token, fn)
}

/**
 * Resolves the stored access token and API base URL needed for a raw, manually authenticated
 * `fetch` call to the backend (as opposed to a generated SDK call, which uses
 * {@link runWithDefaultBackendClient} instead). Throws when no token is stored.
 */
export function loadAuthenticatedFetchContext(): {
  token: string
  apiBaseUrl: string
} {
  const stored = loadStoredAccessToken()
  if (!stored) {
    throw new Error(authenticatedBackendCallFailureAdvice.noAccessTokenInConfig)
  }
  const { apiBaseUrl } = getApiConfig()
  return { token: stored.token, apiBaseUrl }
}

/**
 * Parses the JSON `data` field from a successful SDK response. Use only with calls that pass
 * {@link donutSdkOptions} (so failures throw instead of returning an error envelope).
 */
export async function runDefaultBackendJson<T>(
  fn: () => Promise<unknown>
): Promise<T> {
  const envelope = await runWithDefaultBackendClient(fn)
  return (envelope as { data: T }).data
}

function attachBookFileBlobType(
  format: AttachBookRequestFull['format']
): string {
  return format === 'pdf' ? 'application/pdf' : 'application/epub+zip'
}

export async function attachNotebookBookFile(
  notebookId: number,
  metadata: AttachBookRequestFull,
  absolutePath: string,
  signal?: AbortSignal
): Promise<BookFull> {
  const { token, apiBaseUrl } = loadAuthenticatedFetchContext()
  const fileBytes = await readFile(absolutePath)
  const form = new FormData()
  form.append(
    'metadata',
    new Blob([JSON.stringify(metadata)], { type: 'application/json' })
  )
  form.append(
    'file',
    new Blob([fileBytes], { type: attachBookFileBlobType(metadata.format) }),
    basename(absolutePath)
  )

  return withBackendClient(token, async () => {
    const res = await fetch(
      `${apiBaseUrl}/api/notebooks/${notebookId}/attach-book`,
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
        signal,
      }
    )
    if (!res.ok) {
      const text = await res.text()
      let parsed: unknown
      try {
        parsed = JSON.parse(text) as unknown
      } catch {
        parsed = text
      }
      if (
        typeof parsed === 'object' &&
        parsed !== null &&
        !Array.isArray(parsed)
      ) {
        throw { ...(parsed as Record<string, unknown>), status: res.status }
      }
      throw { body: parsed, status: res.status }
    }
    return (await res.json()) as BookFull
  })
}
