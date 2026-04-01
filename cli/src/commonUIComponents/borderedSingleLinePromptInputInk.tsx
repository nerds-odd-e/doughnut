import stringWidth from 'string-width'
import { Spinner } from '@inkjs/ui'
import type { DOMElement } from 'ink'
import { Box, Text, useCursor } from 'ink'
import { useLayoutEffect, useRef, useState } from 'react'
import { getAbsoluteContentPosition } from '../inkAbsoluteContentPosition.js'
import { truncateToTerminalColumns } from '../terminalColumns.js'

const BORDER_HORIZONTAL_COLS = 2
const CARET_DISPLAY_COLS = 1
/** Spinner frame + gap before label in @inkjs/ui Spinner row. */
const BUSY_SPINNER_RESERVE_COLS = 2
const DEFAULT_LEADING_PREFIX = '→ '

function displayedPlaceholder(
  placeholder: string,
  outerWidthCols: number,
  leadingPrefix: string
): string {
  if (placeholder === '') return ''
  const inner = Math.max(1, outerWidthCols - BORDER_HORIZONTAL_COLS)
  const prefixW = stringWidth(leadingPrefix)
  const maxPlaceholder = Math.max(0, inner - prefixW - CARET_DISPLAY_COLS)
  if (maxPlaceholder < 1) return ''
  return truncateToTerminalColumns(placeholder, maxPlaceholder)
}

function displayedBusyLabel(
  busyLabel: string,
  outerWidthCols: number,
  leadingPrefix: string
): string {
  const inner = Math.max(1, outerWidthCols - BORDER_HORIZONTAL_COLS)
  const prefixW = stringWidth(leadingPrefix)
  const maxLabel = Math.max(0, inner - prefixW - BUSY_SPINNER_RESERVE_COLS)
  if (maxLabel < 1) return ''
  return truncateToTerminalColumns(busyLabel, maxLabel)
}

export function BorderedSingleLinePromptInputInk({
  terminalColumns,
  buffer,
  caretOffset,
  placeholder,
  leadingPrefix = DEFAULT_LEADING_PREFIX,
  busyLabel,
}: {
  readonly terminalColumns: number
  readonly buffer: string
  readonly caretOffset: number
  readonly placeholder: string
  readonly leadingPrefix?: string
  readonly busyLabel?: string
}) {
  const contentOriginRef = useRef<DOMElement | null>(null)
  const [contentOrigin, setContentOrigin] = useState<{
    x: number
    y: number
  } | null>(null)
  const { setCursorPosition } = useCursor()

  const busy = busyLabel !== undefined && busyLabel !== ''

  const truncatedBusyLabel =
    busy && busyLabel !== undefined
      ? displayedBusyLabel(busyLabel, terminalColumns, leadingPrefix)
      : ''

  const placeholderText =
    !busy && buffer === ''
      ? displayedPlaceholder(placeholder, terminalColumns, leadingPrefix)
      : ''

  useLayoutEffect(() => {
    const el = contentOriginRef.current
    if (!el) return
    const p = getAbsoluteContentPosition(el)
    setContentOrigin(p ?? null)
  }, [
    terminalColumns,
    buffer,
    placeholder,
    leadingPrefix,
    busy,
    truncatedBusyLabel,
  ])

  if (!busy && contentOrigin) {
    setCursorPosition({
      x:
        contentOrigin.x +
        stringWidth(leadingPrefix + buffer.slice(0, caretOffset)),
      y: contentOrigin.y,
    })
  } else {
    setCursorPosition(undefined)
  }

  return (
    <Box width={terminalColumns} borderStyle="single" borderColor="white">
      <Box ref={contentOriginRef} flexDirection="row">
        {busy ? (
          <>
            <Text>{leadingPrefix}</Text>
            <Spinner label={truncatedBusyLabel} />
          </>
        ) : (
          <Text>
            {leadingPrefix}
            {buffer}
            {placeholderText !== '' ? (
              <Text color="gray">{placeholderText}</Text>
            ) : null}
          </Text>
        )}
      </Box>
    </Box>
  )
}
