import { afterEach, describe, expect, test } from 'vitest'
import { createServerContext } from '../src/context.js'

describe('createServerContext', () => {
  const previousToken = process.env.DONUT_API_AUTH_TOKEN

  afterEach(() => {
    if (previousToken === undefined) delete process.env.DONUT_API_AUTH_TOKEN
    else process.env.DONUT_API_AUTH_TOKEN = previousToken
  })

  test('reads auth token from DONUT_API_AUTH_TOKEN', () => {
    process.env.DONUT_API_AUTH_TOKEN = 'mcp-auth-token'
    expect(createServerContext().authToken).toBe('mcp-auth-token')
  })
})
