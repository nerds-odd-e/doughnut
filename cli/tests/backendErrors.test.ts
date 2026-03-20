import { describe, expect, test } from 'vitest'
import { userMessageFromBackendError } from '../src/backendErrors.js'

describe('userMessageFromBackendError', () => {
  test('classifies service unavailable for network error', () => {
    expect(
      userMessageFromBackendError(new TypeError('fetch failed: ECONNREFUSED'))
    ).toContain('Doughnut service is not available')
  })

  test('classifies invalid token on 401', () => {
    expect(userMessageFromBackendError({ status: 401 })).toContain(
      'Access token is invalid or expired'
    )
  })

  test('classifies no permission on 403', () => {
    expect(userMessageFromBackendError({ status: 403 })).toContain(
      'Access token does not have permission'
    )
  })
})
