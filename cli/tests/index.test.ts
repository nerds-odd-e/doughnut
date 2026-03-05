import { describe, test, expect } from 'vitest'
import { getGreeting } from '../src/greet.js'

describe('CLI', () => {
  test('returns Hello World as greeting', () => {
    expect(getGreeting()).toBe('Hello World')
  })
})
