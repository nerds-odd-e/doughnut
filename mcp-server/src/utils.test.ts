import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import {
  createErrorResponse,
  validateNoteUpdateParams,
  getEnvironmentConfig,
} from './utils.js'

describe('createErrorResponse', () => {
  test('should handle Error objects', () => {
    const error = new Error('Test error')
    const result = createErrorResponse(error)

    expect(result).toEqual({
      content: [
        {
          type: 'text',
          text: 'ERROR: Test error',
        },
      ],
    })
  })

  test('should handle string errors', () => {
    const error = 'String error'
    const result = createErrorResponse(error)

    expect(result).toEqual({
      content: [
        {
          type: 'text',
          text: 'ERROR: String error',
        },
      ],
    })
  })

  test('should handle unknown error types', () => {
    const error = { code: 500, message: 'Server error' }
    const result = createErrorResponse(error)

    expect(result).toEqual({
      content: [
        {
          type: 'text',
          text: 'ERROR: {"code":500,"message":"Server error"}',
        },
      ],
    })
  })

  test('should use custom prefix', () => {
    const error = 'Test error'
    const result = createErrorResponse(error, 'CUSTOM:')

    expect(result).toEqual({
      content: [
        {
          type: 'text',
          text: 'CUSTOM: Test error',
        },
      ],
    })
  })
})

describe('validateNoteUpdateParams', () => {
  test('should validate valid parameters with title', () => {
    const result = validateNoteUpdateParams(123, 'New title', null)
    expect(result).toEqual({ isValid: true })
  })

  test('should validate valid parameters with details', () => {
    const result = validateNoteUpdateParams(123, null, 'New details')
    expect(result).toEqual({ isValid: true })
  })

  test('should validate valid parameters with both title and details', () => {
    const result = validateNoteUpdateParams(123, 'New title', 'New details')
    expect(result).toEqual({ isValid: true })
  })

  test('should reject undefined noteId', () => {
    const result = validateNoteUpdateParams(undefined, 'New title', null)
    expect(result).toEqual({
      isValid: false,
      error: 'noteId must be a number',
    })
  })

  test('should reject when both title and details are missing', () => {
    const result = validateNoteUpdateParams(123, null, null)
    expect(result).toEqual({
      isValid: false,
      error: 'At least one of newTitle or newDetails must be provided.',
    })
  })

  test('should reject when both title and details are undefined', () => {
    const result = validateNoteUpdateParams(123, undefined, undefined)
    expect(result).toEqual({
      isValid: false,
      error: 'At least one of newTitle or newDetails must be provided.',
    })
  })
})

describe('getEnvironmentConfig', () => {
  let originalApiBaseUrl: string | undefined
  let originalAuthToken: string | undefined

  beforeEach(() => {
    // Store original environment variables
    originalApiBaseUrl = process.env.DOUGHNUT_API_BASE_URL
    originalAuthToken = process.env.DOUGHNUT_API_AUTH_TOKEN
  })

  afterEach(() => {
    // Restore original environment variables
    if (originalApiBaseUrl !== undefined) {
      process.env.DOUGHNUT_API_BASE_URL = originalApiBaseUrl
    } else {
      delete process.env.DOUGHNUT_API_BASE_URL
    }

    if (originalAuthToken !== undefined) {
      process.env.DOUGHNUT_API_AUTH_TOKEN = originalAuthToken
    } else {
      delete process.env.DOUGHNUT_API_AUTH_TOKEN
    }
  })

  test('should return default config when environment variables are not set', () => {
    delete process.env.DOUGHNUT_API_BASE_URL
    delete process.env.DOUGHNUT_API_AUTH_TOKEN

    const config = getEnvironmentConfig()

    expect(config).toEqual({
      apiBaseUrl: 'http://localhost:9081',
      authToken: undefined,
    })
  })

  test('should return custom config when environment variables are set', () => {
    process.env.DOUGHNUT_API_BASE_URL = 'https://api.example.com'
    process.env.DOUGHNUT_API_AUTH_TOKEN = 'test-token'

    const config = getEnvironmentConfig()

    expect(config).toEqual({
      apiBaseUrl: 'https://api.example.com',
      authToken: 'test-token',
    })
  })
})
