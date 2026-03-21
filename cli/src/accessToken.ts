import * as fs from 'node:fs'
import * as path from 'node:path'
import {
  configureClient,
  getApiConfig,
  UserController,
  type GeneratedTokenDto,
  type RequestOptions,
  type TokenConfigDto,
  type UserToken,
} from 'doughnut-api'
import { getConfigDir } from './configDir.js'
import { isFetchAbortedByCaller } from './fetchAbort.js'

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

/**
 * What we tell the user after {@link withBackendClient} catches a throwable from the SDK
 * (`throwOnError: true`). Distinct from “no token in local config” (see
 * {@link authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig}).
 */
const authenticatedBackendCallFailureAdvice = {
  noDefaultTokenInConfig:
    'No default access token. Add one first with /add-access-token.',
  serviceUnreachableOrUnclassifiedFailure:
    'Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.',
  http401StoredTokenRejected:
    'Access token is invalid or expired. Run doughnut login or add a new token with /add-access-token.',
  http403StoredTokenForbidden:
    'Access token does not have permission for this operation. Contact support if you believe this is an error.',
  http502Upstream: 'A dependency service failed (HTTP 502). Try again later.',
  http503Unavailable:
    'The service is temporarily unavailable (HTTP 503). Try again later.',
  http504Timeout: 'The request timed out (HTTP 504). Try again.',
  http5xxServer: 'The server returned an error. Try again later.',
  http400BadRequest:
    'The server rejected this request. Check your input or try again in the web app.',
} as const

/** Backend `ApiError.errorType` values (JSON body when the SDK uses `throwOnError: true`). */
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

/** Values `throwOnError` may throw: parsed JSON body, string body, `TypeError: fetch failed`, etc. */
type SdkThrowable = unknown

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

export type AccessTokenEntry = Pick<GeneratedTokenDto, 'label' | 'token'>
export type AccessTokenLabel = TokenConfigDto['label']

interface AccessTokenConfig {
  tokens: AccessTokenEntry[]
  defaultLabel?: AccessTokenLabel
}

function getConfigPath(): string {
  return path.join(getConfigDir(), 'access-tokens.json')
}

function loadConfig(): AccessTokenConfig {
  try {
    const data = fs.readFileSync(getConfigPath(), 'utf-8')
    const parsed = JSON.parse(data) as Partial<AccessTokenConfig>
    return { tokens: parsed.tokens ?? [], defaultLabel: parsed.defaultLabel }
  } catch {
    return { tokens: [] }
  }
}

function saveConfig(config: AccessTokenConfig): void {
  const p = getConfigPath()
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, JSON.stringify(config, null, 2), 'utf-8')
}

async function withBackendClient<T>(
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

export async function runWithDefaultBackendClient<T>(
  fn: () => Promise<T>
): Promise<T> {
  const config = loadConfig()
  const label = getDefaultTokenLabel()
  const entry = config.tokens.find((t) => t.label === label)
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

/** Like {@link runDefaultBackendJson}, but authenticates with the given bearer token string. */
export async function withBackendJson<T>(
  bearerToken: string,
  fn: () => Promise<unknown>
): Promise<T> {
  const envelope = await withBackendClient(bearerToken, fn)
  return (envelope as { data: T }).data
}

export async function addAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const identity = await withBackendJson<UserToken>(token, () =>
    UserController.getTokenInfo(doughnutSdkOptions(signal))
  )
  const config = loadConfig()
  if (config.tokens.some((t) => t.token === token)) {
    throw new Error('Token already added.')
  }
  config.tokens.push({ label: identity.label, token })
  saveConfig(config)
}

export function listAccessTokens(): AccessTokenEntry[] {
  return loadConfig().tokens
}

export function removeAccessToken(label: AccessTokenLabel): boolean {
  const config = loadConfig()
  const index = config.tokens.findIndex((t) => t.label === label)
  if (index === -1) return false
  config.tokens.splice(index, 1)
  if (config.defaultLabel === label) {
    delete config.defaultLabel
  }
  saveConfig(config)
  return true
}

export async function removeAccessTokenCompletely(
  label: AccessTokenLabel,
  signal?: AbortSignal
): Promise<void> {
  const config = loadConfig()
  const entry = config.tokens.find((t) => t.label === label)
  if (!entry) {
    throw new Error(`Token "${label}" not found.`)
  }
  await withBackendClient(entry.token, () =>
    UserController.revokeToken(doughnutSdkOptions(signal))
  )
  removeAccessToken(label)
}

export function getDefaultTokenLabel(): AccessTokenLabel | undefined {
  const config = loadConfig()
  if (config.tokens.length === 0) return undefined
  if (
    config.defaultLabel &&
    config.tokens.some((t) => t.label === config.defaultLabel)
  ) {
    return config.defaultLabel
  }
  return config.tokens[0]!.label
}

export function setDefaultTokenLabel(label: AccessTokenLabel): void {
  const config = loadConfig()
  config.defaultLabel = label
  saveConfig(config)
}

export async function createAccessToken(
  label: AccessTokenLabel,
  signal?: AbortSignal
): Promise<void> {
  const config = loadConfig()
  const defaultLabel = getDefaultTokenLabel()
  const defaultEntry = config.tokens.find((t) => t.label === defaultLabel)
  if (!defaultEntry) {
    throw new Error(
      authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig
    )
  }
  const body: TokenConfigDto = { label }
  const row = await withBackendJson<GeneratedTokenDto>(defaultEntry.token, () =>
    UserController.generateToken({
      body,
      ...doughnutSdkOptions(signal),
    })
  )
  config.tokens.push({ label: row.label, token: row.token })
  saveConfig(config)
}

export function formatTokenLines(
  tokens: AccessTokenEntry[],
  defaultLabel: AccessTokenLabel | undefined
): string[] {
  return tokens.map((t) => {
    const prefix = t.label === defaultLabel ? '★ ' : '  '
    return `${prefix}${t.label}`
  })
}

export const accessTokenCommandDocs = [
  {
    name: '/add-access-token',
    usage: '/add-access-token',
    description: 'Add a Doughnut access token',
    category: 'interactive' as const,
  },
  {
    name: '/list-access-token',
    usage: '/list-access-token',
    description: 'List stored access tokens',
    category: 'interactive' as const,
  },
  {
    name: '/remove-access-token',
    usage: '/remove-access-token',
    description: 'Remove a local access token',
    category: 'interactive' as const,
  },
  {
    name: '/remove-access-token-completely',
    usage: '/remove-access-token-completely',
    description: 'Remove token locally and from server',
    category: 'interactive' as const,
  },
  {
    name: '/create-access-token',
    usage: '/create-access-token',
    description: 'Create a new access token on the server',
    category: 'interactive' as const,
  },
]
