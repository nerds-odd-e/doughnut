/**
 * Windowed guidance lists for the CLI: fixed row budget, scroll indicators as rows,
 * Ink rendering. Layout is module-private. See ongoing/cli-guidance-list-window.md.
 */

import { useMemo } from 'react'
import { Box, Text } from 'ink'
import {
  formatSlashGuidanceUsageCell,
  slashGuidanceUsageColumnWidth,
} from './mainInteractivePrompt/slashCommandCompletion.js'
import type { NumberedTerminalListLine } from './terminalColumns.js'

const GUIDANCE_LIST_ROW_BUDGET = 5

const GUIDANCE_MORE_ABOVE_LABEL = '↑ more above'
const GUIDANCE_MORE_BELOW_LABEL = '↓ more below'

type SlashGuidanceDisplayRow =
  | { kind: 'moreAbove' }
  | { kind: 'moreBelow' }
  | {
      kind: 'option'
      readonly usage: string
      readonly description: string
      readonly sourceIndex: number
    }

type NumberedGuidanceDisplayRow =
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
  if (budget >= 3) {
    const lastLineOfFirstPage = budget - 2
    if (focus < lastLineOfFirstPage) {
      return {
        innerStart: 0,
        contentRows: budget - 1,
        showTop: false,
        showBottom: true,
      }
    }
    if (focus === lastLineOfFirstPage) {
      return {
        innerStart: 1,
        contentRows: budget - 2,
        showTop: true,
        showBottom: true,
      }
    }
  }
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

function layoutSlashCommandGuidanceWindow(
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

function layoutNumberedListGuidanceWindow(
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

export type GuidanceListInkProps =
  | {
      readonly mode: 'slash'
      readonly rows: readonly {
        readonly usage: string
        readonly description: string
      }[]
      readonly highlightIndex: number
    }
  | {
      readonly mode: 'numbered'
      readonly lines: readonly NumberedTerminalListLine[]
      readonly highlightItemIndex: number
    }

export function GuidanceListInk(props: GuidanceListInkProps) {
  if (props.mode === 'slash') {
    return <SlashGuidanceListInk {...props} />
  }
  return <NumberedGuidanceListInk {...props} />
}

function SlashGuidanceListInk({
  rows,
  highlightIndex,
}: Extract<GuidanceListInkProps, { mode: 'slash' }>) {
  const display = useMemo(
    () =>
      rows.length === 0
        ? null
        : layoutSlashCommandGuidanceWindow(rows, highlightIndex),
    [rows, highlightIndex]
  )

  const usageColWidth = useMemo(() => {
    if (display === null) return 0
    const optionRows = display.filter((r) => r.kind === 'option')
    return slashGuidanceUsageColumnWidth(optionRows)
  }, [display])

  if (display === null || display.length === 0) {
    return null
  }

  const gutter = '  '
  const usageDescGap = '  '

  return display.map((row, i) => {
    if (row.kind === 'moreAbove') {
      return (
        <Text key={`g-up-${i}`} color="gray">
          {gutter}
          {GUIDANCE_MORE_ABOVE_LABEL}
        </Text>
      )
    }
    if (row.kind === 'moreBelow') {
      return (
        <Text key={`g-dn-${i}`} color="gray">
          {gutter}
          {GUIDANCE_MORE_BELOW_LABEL}
        </Text>
      )
    }
    const hi = row.sourceIndex === highlightIndex
    const usageCell = formatSlashGuidanceUsageCell(row.usage, usageColWidth)
    return (
      <Box key={`g-${row.usage}-${row.sourceIndex}`} flexDirection="row">
        <Text>{gutter}</Text>
        {hi ? (
          <>
            <Text bold color="cyan">
              {usageCell}
            </Text>
            <Text bold color="cyan">
              {usageDescGap}
            </Text>
            <Text bold color="cyan">
              {row.description}
            </Text>
          </>
        ) : (
          <>
            <Text color="gray">{usageCell}</Text>
            <Text color="gray">{usageDescGap}</Text>
            <Text color="gray">{row.description}</Text>
          </>
        )}
      </Box>
    )
  })
}

function NumberedGuidanceListInk({
  lines,
  highlightItemIndex,
}: Extract<GuidanceListInkProps, { mode: 'numbered' }>) {
  const display = useMemo(
    () =>
      lines.length === 0
        ? null
        : layoutNumberedListGuidanceWindow(lines, highlightItemIndex),
    [lines, highlightItemIndex]
  )

  if (display === null || display.length === 0) {
    return null
  }

  return display.map((row, i) => {
    if (row.kind === 'moreAbove') {
      return (
        <Text key={`g-up-${i}`} color="gray">
          {GUIDANCE_MORE_ABOVE_LABEL}
        </Text>
      )
    }
    if (row.kind === 'moreBelow') {
      return (
        <Text key={`g-dn-${i}`} color="gray">
          {GUIDANCE_MORE_BELOW_LABEL}
        </Text>
      )
    }
    const hi = row.itemIndex === highlightItemIndex
    return hi ? (
      <Text key={`g-${row.itemIndex}-${i}`} bold color="cyan">
        {row.text}
      </Text>
    ) : (
      <Text key={`g-${row.itemIndex}-${i}`}>{row.text}</Text>
    )
  })
}
