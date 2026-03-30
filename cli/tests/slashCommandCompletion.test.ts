import { describe, expect, test } from 'vitest'
import {
  SLASH_GUIDANCE_USAGE_COL_CAP,
  formatSlashGuidanceUsageCell,
  slashGuidanceUsageColumnWidth,
  slashGuidanceUsageWiderThanCap,
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

  test('wide usage cell is not padded', () => {
    const col = slashGuidanceUsageColumnWidth(
      [{ usage: '/x' }],
      SLASH_GUIDANCE_USAGE_COL_CAP
    )
    expect(
      formatSlashGuidanceUsageCell(
        '/remove-access-token <label>',
        col,
        SLASH_GUIDANCE_USAGE_COL_CAP
      )
    ).toBe('/remove-access-token <label>')
  })

  test('narrow usage is padded to column width', () => {
    expect(
      formatSlashGuidanceUsageCell('/help', 10, SLASH_GUIDANCE_USAGE_COL_CAP)
    ).toBe('/help     ')
  })

  test('widerThanCap matches string width vs cap', () => {
    expect(
      slashGuidanceUsageWiderThanCap('/help', SLASH_GUIDANCE_USAGE_COL_CAP)
    ).toBe(false)
    expect(
      slashGuidanceUsageWiderThanCap(
        '/remove-access-token <label>',
        SLASH_GUIDANCE_USAGE_COL_CAP
      )
    ).toBe(true)
  })
})
