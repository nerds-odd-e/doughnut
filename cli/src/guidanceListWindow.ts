/**
 * Fixed-height guidance list window: scroll indicators replace option rows
 * (same row budget), never add rows below the cap. See ongoing/cli-guidance-list-window.md.
 */

import type { NumberedTerminalListLine } from './terminalColumns.js'

export const GUIDANCE_LIST_ROW_BUDGET = 5

export const GUIDANCE_MORE_ABOVE_LABEL = '↑ more above'
export const GUIDANCE_MORE_BELOW_LABEL = '↓ more below'

export type SlashGuidanceDisplayRow =
  | { kind: 'moreAbove' }
  | { kind: 'moreBelow' }
  | {
      kind: 'option'
      readonly usage: string
      readonly description: string
      readonly sourceIndex: number
    }

export type NumberedGuidanceDisplayRow =
  | { kind: 'moreAbove' }
  | { kind: 'moreBelow' }
  | {
      kind: 'option'
      readonly itemIndex: number
      readonly text: string
    }

function layoutWindowedLineSlice(
  lineCount: number,
  focusLineIndex: number,
  budget: number
): {
  innerStart: number
  contentRows: number
  showTop: boolean
  showBottom: boolean
} {
  if (lineCount <= budget) {
    return {
      innerStart: 0,
      contentRows: lineCount,
      showTop: false,
      showBottom: false,
    }
  }
  const focus = Math.max(0, Math.min(focusLineIndex, lineCount - 1))
  for (const showTop of [false, true] as const) {
    for (const showBottom of [false, true] as const) {
      const t = showTop ? 1 : 0
      const b = showBottom ? 1 : 0
      const c = budget - t - b
      if (c < 1 || c > lineCount) continue
      const innerStart = Math.min(
        Math.max(0, focus - Math.floor(c / 2)),
        lineCount - c
      )
      const actualTop = innerStart > 0
      const actualBottom = innerStart + c < lineCount
      if (actualTop === showTop && actualBottom === showBottom) {
        return {
          innerStart,
          contentRows: c,
          showTop,
          showBottom,
        }
      }
    }
  }
  const c = Math.min(budget, lineCount)
  const innerStart = Math.min(
    Math.max(0, focus - Math.floor(c / 2)),
    lineCount - c
  )
  return {
    innerStart,
    contentRows: c,
    showTop: false,
    showBottom: false,
  }
}

export function layoutSlashCommandGuidanceWindow(
  rows: readonly { readonly usage: string; readonly description: string }[],
  highlightIndex: number,
  budget: number = GUIDANCE_LIST_ROW_BUDGET
): readonly SlashGuidanceDisplayRow[] {
  const n = rows.length
  if (n === 0) return []
  const focus = Math.max(0, Math.min(highlightIndex, n - 1))
  const { innerStart, contentRows, showTop, showBottom } =
    layoutWindowedLineSlice(n, focus, budget)
  const out: SlashGuidanceDisplayRow[] = []
  if (showTop) out.push({ kind: 'moreAbove' })
  for (let i = 0; i < contentRows; i++) {
    const idx = innerStart + i
    const row = rows[idx]!
    out.push({
      kind: 'option',
      usage: row.usage,
      description: row.description,
      sourceIndex: idx,
    })
  }
  if (showBottom) out.push({ kind: 'moreBelow' })
  return out
}

function firstLineIndexForItem(
  lines: readonly NumberedTerminalListLine[],
  itemIndex: number
): number {
  const i = lines.findIndex((l) => l.itemIndex === itemIndex)
  return i === -1 ? 0 : i
}

export function layoutNumberedListGuidanceWindow(
  lines: readonly NumberedTerminalListLine[],
  highlightItemIndex: number,
  budget: number = GUIDANCE_LIST_ROW_BUDGET
): readonly NumberedGuidanceDisplayRow[] {
  const n = lines.length
  if (n === 0) return []
  const focus = firstLineIndexForItem(lines, highlightItemIndex)
  const { innerStart, contentRows, showTop, showBottom } =
    layoutWindowedLineSlice(n, focus, budget)
  const out: NumberedGuidanceDisplayRow[] = []
  if (showTop) out.push({ kind: 'moreAbove' })
  for (let i = 0; i < contentRows; i++) {
    const line = lines[innerStart + i]!
    out.push({
      kind: 'option',
      itemIndex: line.itemIndex,
      text: line.text,
    })
  }
  if (showBottom) out.push({ kind: 'moreBelow' })
  return out
}
