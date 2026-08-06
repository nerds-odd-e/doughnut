import { describe, test, expect } from 'vitest'
import { createErrorResponse } from '../src/helpers.js'

describe('createErrorResponse', () => {
  test('formats an Error message with the default prefix', () => {
    expect(createErrorResponse(new Error('Test error'))).toEqual({
      content: [{ type: 'text', text: 'ERROR: Test error' }],
    })
  })

  test.each([
    ['string', 'String error', 'ERROR: String error'],
    [
      'unknown object',
      { code: 500, message: 'Server error' },
      'ERROR: {"code":500,"message":"Server error"}',
    ],
  ])('formats a %s with the default prefix', (_, error, expectedText) => {
    expect(createErrorResponse(error).content[0].text).toBe(expectedText)
  })

  test('uses a custom prefix when provided', () => {
    expect(createErrorResponse('Test error', 'CUSTOM:').content[0].text).toBe(
      'CUSTOM: Test error'
    )
  })
})
