import { describe, expect, test } from 'vitest'
import { userVisibleSlashCommandError } from '../src/userVisibleSlashCommandError.js'

describe('userVisibleSlashCommandError', () => {
  test('AbortError maps to Cancelled', () => {
    expect(
      userVisibleSlashCommandError(new DOMException('aborted', 'AbortError'))
    ).toBe('Cancelled.')
    const err = new Error('aborted')
    err.name = 'AbortError'
    expect(userVisibleSlashCommandError(err)).toBe('Cancelled.')
  })

  test('Error uses message', () => {
    expect(userVisibleSlashCommandError(new Error('network down'))).toBe(
      'network down'
    )
  })

  test('non-Error coerces to string', () => {
    expect(userVisibleSlashCommandError(503)).toBe('503')
  })
})
