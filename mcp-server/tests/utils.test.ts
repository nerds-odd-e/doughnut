import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  createErrorResponse,
  validateNoteUpdateParams,
  getEnvironmentConfig,
} from '../src/utils.js'

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

  test('should reject when noteId is not a number', () => {
    const result = validateNoteUpdateParams(
      '123' as unknown as number,
      'New title',
      null
    )
    expect(result).toEqual({
      isValid: false,
      error: 'noteId must be a number',
    })
  })

  test('should reject when noteId is null', () => {
    const result = validateNoteUpdateParams(
      null as unknown as number,
      'New title',
      null
    )
    expect(result).toEqual({
      isValid: false,
      error: 'noteId must be a number',
    })
  })

  test('should accept noteId of 0', () => {
    const result = validateNoteUpdateParams(0, 'New title', null)
    expect(result).toEqual({ isValid: true })
  })

  test('should accept negative noteId', () => {
    const result = validateNoteUpdateParams(-1, 'New title', null)
    expect(result).toEqual({ isValid: true })
  })

  test('should accept empty string for title', () => {
    const result = validateNoteUpdateParams(123, '', null)
    expect(result).toEqual({ isValid: true })
  })

  test('should accept empty string for details', () => {
    const result = validateNoteUpdateParams(123, null, '')
    expect(result).toEqual({ isValid: true })
  })

  test('should accept whitespace-only title', () => {
    const result = validateNoteUpdateParams(123, '   ', null)
    expect(result).toEqual({ isValid: true })
  })

  test('should accept whitespace-only details', () => {
    const result = validateNoteUpdateParams(123, null, '   ')
    expect(result).toEqual({ isValid: true })
  })
})

describe('getEnvironmentConfig', () => {
  const originalEnv = process.env

  beforeEach(() => {
    vi.resetModules()
    process.env = { ...originalEnv }
  })

  afterEach(() => {
    process.env = originalEnv
  })

  test('should return config with valid environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = 'http://localhost:8080'
    process.env.DOUGHNUT_API_AUTH_TOKEN = 'test-token'

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:8080')
    expect(config.authToken).toBe('test-token')
  })

  test('should return config with missing environment variables', () => {
    delete process.env.DOUGHNUT_API_BASE_URL
    delete process.env.DOUGHNUT_API_AUTH_TOKEN

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:9081') // Default value
    expect(config.authToken).toBeUndefined()
  })

  test('should return config with empty environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = ''
    process.env.DOUGHNUT_API_AUTH_TOKEN = ''

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:9081') // Empty string is falsy, so default is used
    expect(config.authToken).toBe('')
  })

  test('should return config with undefined environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = undefined
    process.env.DOUGHNUT_API_AUTH_TOKEN = undefined

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:9081') // Default value
    expect(config.authToken).toBeUndefined()
  })

  test('should return config with null environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = null as unknown as string
    process.env.DOUGHNUT_API_AUTH_TOKEN = null as unknown as string

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:9081') // Default value
    expect(config.authToken).toBeNull()
  })

  test('should return config with mixed environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = 'http://localhost:8080'
    process.env.DOUGHNUT_API_AUTH_TOKEN = undefined

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('http://localhost:8080')
    expect(config.authToken).toBeUndefined()
  })

  test('should return config with whitespace environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = '  http://localhost:8080  '
    process.env.DOUGHNUT_API_AUTH_TOKEN = '  test-token  '

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('  http://localhost:8080  ')
    expect(config.authToken).toBe('  test-token  ')
  })

  test('should return config with special characters in environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL =
      'https://api.example.com:443/path?query=value'
    process.env.DOUGHNUT_API_AUTH_TOKEN =
      'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe(
      'https://api.example.com:443/path?query=value'
    )
    expect(config.authToken).toBe('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9')
  })

  test('should return config with very long environment variables', () => {
    const longUrl = `http://localhost:8080/${'a'.repeat(1000)}`
    const longToken = 'a'.repeat(1000)

    process.env.DOUGHNUT_API_BASE_URL = longUrl
    process.env.DOUGHNUT_API_AUTH_TOKEN = longToken

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe(longUrl)
    expect(config.authToken).toBe(longToken)
  })

  test('should return config with numeric environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = '8080'
    process.env.DOUGHNUT_API_AUTH_TOKEN = '12345'

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('8080')
    expect(config.authToken).toBe('12345')
  })

  test('should return config with boolean-like environment variables', () => {
    process.env.DOUGHNUT_API_BASE_URL = 'true'
    process.env.DOUGHNUT_API_AUTH_TOKEN = 'false'

    const config = getEnvironmentConfig()

    expect(config.apiBaseUrl).toBe('true')
    expect(config.authToken).toBe('false')
  })
})
