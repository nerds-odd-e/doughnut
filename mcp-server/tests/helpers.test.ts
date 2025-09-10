import { describe, test, expect, beforeEach, afterEach, vi } from 'vitest'
import { createErrorResponse, getEnvironmentConfig } from '../src/helpers.js'

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
})
