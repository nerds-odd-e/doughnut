import { describe, expect, test } from 'vitest'
import { slashGuidanceUsageColumnWidth } from '../src/mainInteractivePrompt/slashCommandCompletion.js'

describe('slash guidance usage column cap', () => {
  test('column width ignores usages wider than cap', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/short' }, { usage: '/very-long-command-name <arg>' }],
        26
      )
    ).toBe(6)
  })

  test('column width is capped', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/a' }, { usage: '/medium-length' }],
        26
      )
    ).toBe(14)
  })
})
