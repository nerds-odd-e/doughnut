/**
 * Ink rendering for windowed guidance lists (layout + scroll indicators).
 * Call sites use this component instead of importing layout helpers and labels separately.
 */

import { useMemo } from 'react'
import { Text } from 'ink'
import {
  GUIDANCE_MORE_ABOVE_LABEL,
  GUIDANCE_MORE_BELOW_LABEL,
  layoutNumberedListGuidanceWindow,
  layoutSlashCommandGuidanceWindow,
} from './guidanceListWindow.js'
import type { NumberedTerminalListLine } from './terminalColumns.js'

export type GuidanceListInkProps =
  | {
      readonly mode: 'slash'
      readonly rows: readonly {
        readonly usage: string
        readonly description: string
      }[]
      readonly highlightIndex: number
      readonly usagePad: number
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
  usagePad,
}: Extract<GuidanceListInkProps, { mode: 'slash' }>) {
  const display = useMemo(
    () =>
      rows.length === 0
        ? null
        : layoutSlashCommandGuidanceWindow(rows, highlightIndex),
    [rows, highlightIndex]
  )

  if (display === null || display.length === 0) {
    return null
  }

  const gutter = '  '

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
    return (
      <Text key={`g-${row.usage}-${row.sourceIndex}`}>
        {gutter}
        {hi ? (
          <Text inverse>
            {row.usage.padEnd(usagePad)}
            {row.description}
          </Text>
        ) : (
          <Text color="gray">
            {row.usage.padEnd(usagePad)}
            {row.description}
          </Text>
        )}
      </Text>
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
    return (
      <Text
        key={`g-${row.itemIndex}-${i}`}
        inverse={row.itemIndex === highlightItemIndex}
      >
        {row.text}
      </Text>
    )
  })
}
