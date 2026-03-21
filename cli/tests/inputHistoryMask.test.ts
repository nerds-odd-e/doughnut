import { describe, expect, test } from 'vitest'
import { maskInteractiveInputForHistory } from '../src/inputHistoryMask.js'

describe('maskInteractiveInputForHistory', () => {
  test('redacts token for exact /add-access-token command', () => {
    expect(maskInteractiveInputForHistory('/add-access-token secret')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('normalizes casing on the command when masking', () => {
    expect(maskInteractiveInputForHistory('/ADD-ACCESS-TOKEN mytok')).toBe(
      '/add-access-token <redacted>'
    )
    expect(maskInteractiveInputForHistory('/Add-Access-Token mytok')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('extra spaces around token still redact', () => {
    expect(maskInteractiveInputForHistory('  /add-access-token   x  ')).toBe(
      '/add-access-token <redacted>'
    )
  })

  test('does not match a longer slash command name', () => {
    expect(
      maskInteractiveInputForHistory('/add-access-tokenx not-a-real-cmd')
    ).toBe('/add-access-tokenx not-a-real-cmd')
  })

  test('leaves usage-only lines unchanged', () => {
    expect(maskInteractiveInputForHistory('/add-access-token')).toBe(
      '/add-access-token'
    )
    expect(maskInteractiveInputForHistory('/add-access-token ')).toBe(
      '/add-access-token '
    )
    expect(maskInteractiveInputForHistory('/add-access-token   ')).toBe(
      '/add-access-token   '
    )
  })

  test('leaves other commands and plain text unchanged', () => {
    expect(maskInteractiveInputForHistory('/help')).toBe('/help')
    expect(maskInteractiveInputForHistory('hello')).toBe('hello')
  })

  test('masks a matching line in a multiline draft', () => {
    expect(
      maskInteractiveInputForHistory('note\n/add-access-token t\nmore')
    ).toBe('note\n/add-access-token <redacted>\nmore')
  })
})
