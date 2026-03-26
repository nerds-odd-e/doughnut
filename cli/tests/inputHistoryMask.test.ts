import { describe, expect, test } from 'vitest'
import { maskInteractiveInputLineForStorage } from '../src/inputHistoryMask.js'

describe('maskInteractiveInputLineForStorage', () => {
  test('redacts token for exact /add-access-token command', () => {
    expect(maskInteractiveInputLineForStorage('/add-access-token secret')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('normalizes casing on the command when masking', () => {
    expect(maskInteractiveInputLineForStorage('/ADD-ACCESS-TOKEN mytok')).toBe(
      '/add-access-token <redacted>'
    )
    expect(maskInteractiveInputLineForStorage('/Add-Access-Token mytok')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('extra spaces around token still redact', () => {
    expect(
      maskInteractiveInputLineForStorage('  /add-access-token   x  ')
    ).toBe('/add-access-token <redacted>')
  })

  test('does not match a longer slash command name', () => {
    expect(
      maskInteractiveInputLineForStorage('/add-access-tokenx not-a-real-cmd')
    ).toBe('/add-access-tokenx not-a-real-cmd')
  })

  test('leaves usage-only lines unchanged', () => {
    expect(maskInteractiveInputLineForStorage('/add-access-token')).toBe(
      '/add-access-token'
    )
    expect(maskInteractiveInputLineForStorage('/add-access-token ')).toBe(
      '/add-access-token '
    )
    expect(maskInteractiveInputLineForStorage('/add-access-token   ')).toBe(
      '/add-access-token   '
    )
  })

  test('leaves other commands and plain text unchanged', () => {
    expect(maskInteractiveInputLineForStorage('/help')).toBe('/help')
    expect(maskInteractiveInputLineForStorage('hello')).toBe('hello')
  })

  test('masks a matching line in a multiline draft', () => {
    expect(
      maskInteractiveInputLineForStorage('note\n/add-access-token t\nmore')
    ).toBe('note\n/add-access-token <redacted>\nmore')
  })
})
