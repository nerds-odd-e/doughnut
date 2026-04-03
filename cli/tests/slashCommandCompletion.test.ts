import { describe, expect, test } from 'vitest'
import {
  SLASH_GUIDANCE_USAGE_COL_CAP,
  slashGuidanceUsageColumnWidth,
} from '../src/mainInteractivePrompt/slashCommandCompletion.js'

describe('slash guidance usage column cap', () => {
  test('column width ignores usages wider than cap', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/short' }, { usage: '/very-long-command-name <arg>' }],
        SLASH_GUIDANCE_USAGE_COL_CAP
      )
    ).toBe(6)
  })

  test('column width is capped', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/a' }, { usage: '/medium-length' }],
        SLASH_GUIDANCE_USAGE_COL_CAP
      )
    ).toBe(14)
  })
})
