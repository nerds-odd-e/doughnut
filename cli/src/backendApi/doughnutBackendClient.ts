import { readFile } from 'node:fs/promises'
import { basename } from 'node:path'
import type { AttachBookRequestFull } from '../../../packages/generated/doughnut-backend-api/types.gen.js'
import {
  configureClient,
  getApiConfig,
  type BookFull,
  type RequestOptions,
} from 'doughnut-api'
import {
  defaultAccessTokenLabel,
  loadAccessTokenConfig,
} from './accessTokenStorage.js'

/**
 * For every call to the generated Doughnut HTTP client that runs inside
 * {@link withBackendClient} / {@link runWithDefaultBackendClient}: non-OK responses and
 * fetch failures throw instead of returning `{ error }`.
 */
export type DoughnutSdkCallOptions = Partial<Pick<RequestOptions, 'signal'>> & {
  throwOnError: true
}

export function doughnutSdkOptions(
  signal?: AbortSignal
): DoughnutSdkCallOptions {
  return signal === undefined
    ? { throwOnError: true }
    : { throwOnError: true, signal }
}

const authenticatedBackendCallFailureAdvice = {
  noDefaultTokenInConfig: 'No default access token. Run doughnut login first.',
  serviceUnreachableOrUnclassifiedFailure:
    'Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.',
  http401StoredTokenRejected:
    'Access token is invalid or expired. Run doughnut login or configure a new token.',
  http403StoredTokenForbidden:
    'Access token does not have permission for this operation. Contact support if you believe this is an error.',
  http502Upstream: 'A dependency service failed (HTTP 502). Try again later.',
  http503Unavailable:
    'The service is temporarily unavailable (HTTP 503). Try again later.',
  http504Timeout: 'The request timed out (HTTP 504). Try again.',
  http5xxServer: 'The server returned an error. Try again later.',
  http400BadRequest:
    'The server rejected this request. Check your input or try again in the web app.',
  http404NotFound:
    'The resource was not found. It may have been removed, or the link is wrong.',
  http409Conflict:
    'This action conflicts with the current state on the server. Contact support if you need help.',
} as const

type SdkThrowable = unknown

const MAX_CLIENT_ERROR_DETAIL_CHARS = 500

function readableClientErrorDetail(cause: SdkThrowable): string | undefined {
  if (typeof cause !== 'object' || cause === null) return undefined
  const o = cause as Record<string, unknown>
  for (const key of ['message', 'detail'] as const) {
    const raw = o[key]
    if (typeof raw !== 'string') continue
    const t = raw.trim()
    if (t === '') continue
    return t.length > MAX_CLIENT_ERROR_DETAIL_CHARS
      ? `${t.slice(0, MAX_CLIENT_ERROR_DETAIL_CHARS)}…`
      : t
  }
  return undefined
}

const backendApiErrorTypes = new Set([
  'OPENAI_UNAUTHORIZED',
  'BINDING_ERROR',
  'OPENAI_TIMEOUT',
  'OPENAI_SERVICE_ERROR',
  'OPENAI_NOT_AVAILABLE',
  'WIKIDATA_SERVICE_ERROR',
  'ASSESSMENT_SERVICE_ERROR',
  'QUESTION_ANSWER_ERROR',
])

function httpStatusFromSdkThrowable(cause: SdkThrowable): number | undefined {
  if (typeof cause !== 'object' || cause === null) return undefined
  const o = cause as Record<string, unknown>
  if (typeof o.status === 'number' && Number.isFinite(o.status)) {
    return o.status
  }
  const res = o.response
  if (typeof res === 'object' && res !== null) {
    const s = (res as Record<string, unknown>).status
    if (typeof s === 'number' && Number.isFinite(s)) return s
  }
  return undefined
}

function doughnutApiErrorFromThrowable(
  cause: SdkThrowable
): { errorType: string; message?: string } | undefined {
  if (typeof cause !== 'object' || cause === null) return undefined
  const o = cause as Record<string, unknown>
  const errorType = o.errorType
  if (typeof errorType !== 'string' || !backendApiErrorTypes.has(errorType)) {
    return undefined
  }
  const raw = o.message
  const message =
    typeof raw === 'string' && raw.trim() !== '' ? raw.trim() : undefined
  return { errorType, message }
}

function messageForHttpServerError(status: number): string {
  if (status === 502)
    return authenticatedBackendCallFailureAdvice.http502Upstream
  if (status === 503)
    return authenticatedBackendCallFailureAdvice.http503Unavailable
  if (status === 504)
    return authenticatedBackendCallFailureAdvice.http504Timeout
  if (status >= 500) return authenticatedBackendCallFailureAdvice.http5xxServer
  return authenticatedBackendCallFailureAdvice.serviceUnreachableOrUnclassifiedFailure
}

function userVisibleMessageForKnownApiErrorWithoutBodyMessage(
  errorType: string,
  statusHint: number | undefined
): string {
  const st = statusHint ?? inferredStatusForBackendErrorType(errorType)
  if (st >= 500) return messageForHttpServerError(st)
  if (st === 400) return authenticatedBackendCallFailureAdvice.http400BadRequest
  return authenticatedBackendCallFailureAdvice.serviceUnreachableOrUnclassifiedFailure
}

function isLikelyTransportLayerFailure(cause: SdkThrowable): boolean {
  if (typeof cause !== 'object' || cause === null) return false
  const code = (cause as NodeJS.ErrnoException).code
  if (
    code === 'ECONNREFUSED' ||
    code === 'ENOTFOUND' ||
    code === 'ETIMEDOUT' ||
    code === 'ECONNRESET'
  ) {
    return true
  }
  if (cause instanceof TypeError && /fetch failed/i.test(cause.message)) {
    return true
  }
  const message = cause instanceof Error ? cause.message : ''
  return /network|timeout|ECONNREFUSED|ENOTFOUND|ETIMEDOUT/i.test(message)
}

function userVisibleMessageForSdkThrowable(cause: SdkThrowable): string {
  if (isLikelyTransportLayerFailure(cause)) {
    return authenticatedBackendCallFailureAdvice.serviceUnreachableOrUnclassifiedFailure
  }
  const apiErr = doughnutApiErrorFromThrowable(cause)
  if (apiErr !== undefined) {
    if (apiErr.message !== undefined) return apiErr.message
    return userVisibleMessageForKnownApiErrorWithoutBodyMessage(
      apiErr.errorType,
      httpStatusFromSdkThrowable(cause)
    )
  }
  const status = httpStatusFromSdkThrowable(cause)
  if (status === 401) {
    return authenticatedBackendCallFailureAdvice.http401StoredTokenRejected
  }
  if (status === 403) {
    return authenticatedBackendCallFailureAdvice.http403StoredTokenForbidden
  }
  if (status === 400) {
    return (
      readableClientErrorDetail(cause) ??
      authenticatedBackendCallFailureAdvice.http400BadRequest
    )
  }
  if (status === 404) {
    return (
      readableClientErrorDetail(cause) ??
      authenticatedBackendCallFailureAdvice.http404NotFound
    )
  }
  if (status === 409) {
    return (
      readableClientErrorDetail(cause) ??
      authenticatedBackendCallFailureAdvice.http409Conflict
    )
  }
  if (status !== undefined && status >= 500) {
    return messageForHttpServerError(status)
  }
  return authenticatedBackendCallFailureAdvice.serviceUnreachableOrUnclassifiedFailure
}

function inferredStatusForBackendErrorType(errorType: string): number {
  switch (errorType) {
    case 'OPENAI_TIMEOUT':
      return 504
    case 'OPENAI_SERVICE_ERROR':
    case 'WIKIDATA_SERVICE_ERROR':
    case 'ASSESSMENT_SERVICE_ERROR':
      return 502
    case 'OPENAI_NOT_AVAILABLE':
      return 503
    case 'OPENAI_UNAUTHORIZED':
    case 'QUESTION_ANSWER_ERROR':
    case 'BINDING_ERROR':
      return 400
    default:
      return 500
  }
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
  const config = loadAccessTokenConfig()
  const label = defaultAccessTokenLabel(config)
  const entry =
    label === undefined
      ? undefined
      : config.tokens.find((t) => t.label === label)
  if (!entry) {
    throw new Error(
      authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig
    )
  }
  return withBackendClient(entry.token, fn)
}

/**
 * Parses the JSON `data` field from a successful SDK response. Use only with calls that pass
 * {@link doughnutSdkOptions} (so failures throw instead of returning an error envelope).
 */
export async function runDefaultBackendJson<T>(
  fn: () => Promise<unknown>
): Promise<T> {
  const envelope = await runWithDefaultBackendClient(fn)
  return (envelope as { data: T }).data
}

export async function attachNotebookBookWithPdf(
  notebookId: number,
  metadata: AttachBookRequestFull,
  pdfAbsolutePath: string,
  signal?: AbortSignal
): Promise<BookFull> {
  const config = loadAccessTokenConfig()
  const label = defaultAccessTokenLabel(config)
  const entry =
    label === undefined
      ? undefined
      : config.tokens.find((t) => t.label === label)
  if (!entry) {
    throw new Error(
      authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig
    )
  }

  const { apiBaseUrl } = getApiConfig()
  const pdfBytes = await readFile(pdfAbsolutePath)
  const form = new FormData()
  form.append(
    'metadata',
    new Blob([JSON.stringify(metadata)], { type: 'application/json' })
  )
  form.append(
    'file',
    new Blob([pdfBytes], { type: 'application/pdf' }),
    basename(pdfAbsolutePath)
  )

  return withBackendClient(entry.token, async () => {
    const res = await fetch(
      `${apiBaseUrl}/api/notebooks/${notebookId}/attach-book`,
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${entry.token}` },
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
