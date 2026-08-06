import { Box } from 'ink'
import { render } from 'ink-testing-library'
import { expect } from 'vitest'
import {
  GuidanceListInk,
  type GuidanceListInkProps,
} from '../src/commonUIComponents/guidanceListWindowInk.js'
import { stripAnsi } from './inkTestHelpers.js'

export const MORE_ABOVE = '↑ more above'
export const MORE_BELOW = '↓ more below'
export const ROW_BUDGET = 5
export const TALL_NUMBERED_ROW_BUDGET = 10

export function renderGuidancePlain(props: GuidanceListInkProps) {
  const { lastFrame } = render(
    <Box flexDirection="column">
      <GuidanceListInk {...props} />
    </Box>
  )
  return stripAnsi(lastFrame() ?? '')
}

export function slashRows(n: number) {
  return Array.from({ length: n }, (_, i) => ({
    usage: `/cmd${i}`,
    description: `d${i}`,
  }))
}

export function expectMoreBelowLastWhenPresent(plain: string) {
  if (!plain.includes(MORE_BELOW)) return
  const lines = plain
    .split('\n')
    .map((l) => l.trimEnd())
    .filter((l) => l.length > 0)
  expect(lines[lines.length - 1]).toContain(MORE_BELOW)
}

export function expectBothScrollIndicatorsBracketOptions(plain: string) {
  if (!(plain.includes(MORE_ABOVE) && plain.includes(MORE_BELOW))) return
  const lines = plain
    .split('\n')
    .map((l) => l.trimEnd())
    .filter((l) => l.length > 0)
  expect(lines).toHaveLength(ROW_BUDGET)
  expect(lines[0]).toContain(MORE_ABOVE)
  expect(lines[lines.length - 1]).toContain(MORE_BELOW)
  for (const mid of lines.slice(1, -1)) {
    expect(mid).not.toContain(MORE_ABOVE)
    expect(mid).not.toContain(MORE_BELOW)
  }
}
