import { describe, expect, test } from 'vitest'
import {
  formatSlashGuidanceUsageCell,
  slashGuidanceUsageColumnWidth,
  slashGuidanceUsageWiderThanCap,
} from '../src/mainInteractivePrompt/slashCommandCompletion.js'

describe('slash guidance usage column cap', () => {
  test('column width ignores usages wider than cap', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/short' }, { usage: '/very-long-command-name <arg>' }],
        22
      )
    ).toBe(6)
  })

  test('column width is capped', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/a' }, { usage: '/medium-length' }],
        22
      )
    ).toBe(14)
  })

  test('wide usage cell is not padded', () => {
    const col = slashGuidanceUsageColumnWidth([{ usage: '/x' }], 22)
    expect(
      formatSlashGuidanceUsageCell('/remove-access-token <label>', col, 22)
    ).toBe('/remove-access-token <label>')
  })

  test('narrow usage is padded to column width', () => {
    expect(formatSlashGuidanceUsageCell('/help', 10, 22)).toBe('/help     ')
  })

  test('widerThanCap matches string width vs cap', () => {
    expect(slashGuidanceUsageWiderThanCap('/help', 22)).toBe(false)
    expect(
      slashGuidanceUsageWiderThanCap('/remove-access-token <label>', 22)
    ).toBe(true)
  })
})
