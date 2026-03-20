const SERVICE_UNAVAILABLE_MESSAGE =
  'Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.'

const INVALID_ACCESS_TOKEN_MESSAGE =
  'Access token is invalid or expired. Run doughnut login or add a new token with /add-access-token.'

const ACCESS_TOKEN_FORBIDDEN_MESSAGE =
  'Access token does not have permission for this operation. Contact support if you believe this is an error.'

function statusFromUnknownError(err: unknown): number | undefined {
  if (typeof err !== 'object' || err === null) return undefined
  const obj = err as Record<string, unknown>
  if (typeof obj.status === 'number') return obj.status
  if (
    typeof obj.response === 'object' &&
    obj.response !== null &&
    typeof (obj.response as Record<string, unknown>).status === 'number'
  ) {
    return (obj.response as Record<string, unknown>).status as number
  }
  return undefined
}

function isNetworkError(err: unknown): boolean {
  if (typeof err !== 'object' || err === null) return false
  const obj = err as Record<string, unknown>
  const code = typeof obj.code === 'string' ? obj.code : ''
  if (
    code === 'ECONNREFUSED' ||
    code === 'ENOTFOUND' ||
    code === 'ETIMEDOUT' ||
    code === 'ECONNRESET'
  ) {
    return true
  }
  const message = typeof obj.message === 'string' ? obj.message : ''
  return /fetch failed|network|timeout|ECONNREFUSED|ENOTFOUND|ETIMEDOUT/i.test(
    message
  )
}

export function userMessageFromBackendError(err: unknown): string {
  if (isNetworkError(err)) return SERVICE_UNAVAILABLE_MESSAGE
  const status = statusFromUnknownError(err)
  if (status === 401) return INVALID_ACCESS_TOKEN_MESSAGE
  if (status === 403) return ACCESS_TOKEN_FORBIDDEN_MESSAGE
  return SERVICE_UNAVAILABLE_MESSAGE
}
