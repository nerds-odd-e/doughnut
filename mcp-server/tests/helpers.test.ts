import { describe, test, expect, beforeEach, afterEach } from 'vitest'
import { createErrorResponse } from '../src/helpers.js'
import { getApiConfig } from 'doughnut-api'

describe('createErrorResponse', () => {
  test.each([
    ['Error objects', new Error('Test error'), 'ERROR:', 'ERROR: Test error'],
    ['string errors', 'String error', 'ERROR:', 'ERROR: String error'],
    [
      'unknown error types',
      { code: 500, message: 'Server error' },
      'ERROR:',
      'ERROR: {"code":500,"message":"Server error"}',
    ],
    ['custom prefix', 'Test error', 'CUSTOM:', 'CUSTOM: Test error'],
  ])('should handle %s', (_, error, prefix, expectedText) => {
    const result = createErrorResponse(error, prefix)

    expect(result).toEqual({
      content: [{ type: 'text', text: expectedText }],
    })
  })
})

describe('getApiConfig', () => {
  const originalEnv = process.env

  beforeEach(() => {
    process.env = { ...originalEnv }
  })

  afterEach(() => {
    process.env = originalEnv
  })

  test.each([
    [
      'valid environment variables',
      {
        DOUGHNUT_API_BASE_URL: 'http://localhost:8080',
        DOUGHNUT_API_AUTH_TOKEN: 'test-token',
      },
      { apiBaseUrl: 'http://localhost:8080', authToken: 'test-token' },
    ],
    [
      'missing environment variables',
      {},
      { apiBaseUrl: 'https://doughnut.odd-e.com', authToken: undefined },
    ],
    [
      'empty environment variables',
      { DOUGHNUT_API_BASE_URL: '', DOUGHNUT_API_AUTH_TOKEN: '' },
      { apiBaseUrl: 'https://doughnut.odd-e.com', authToken: '' },
    ],
  ])('should return config with %s', (_, env, expected) => {
    delete process.env.DOUGHNUT_API_BASE_URL
    delete process.env.DOUGHNUT_API_AUTH_TOKEN
    Object.assign(process.env, env)

    expect(getApiConfig()).toEqual(expected)
  })
})
