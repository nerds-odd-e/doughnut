import stringWidth from 'string-width'
import { Box, Text } from 'ink'
import { truncateToTerminalColumns } from '../terminalColumns.js'

const BORDER_HORIZONTAL_COLS = 2
const CARET_DISPLAY_COLS = 1
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

export function BorderedSingleLinePromptInputInk({
  terminalColumns,
  buffer,
  caretOffset,
  placeholder,
  leadingPrefix = DEFAULT_LEADING_PREFIX,
}: {
  readonly terminalColumns: number
  readonly buffer: string
  readonly caretOffset: number
  readonly placeholder: string
  readonly leadingPrefix?: string
}) {
  const beforeCaret = buffer.slice(0, caretOffset)
  const afterCaret = buffer.slice(caretOffset)
  const placeholderText =
    buffer === ''
      ? displayedPlaceholder(placeholder, terminalColumns, leadingPrefix)
      : ''

  return (
    <Box width={terminalColumns} borderStyle="single" borderColor="white">
      <Text>
        {leadingPrefix}
        {beforeCaret}
        <Text inverse> </Text>
        {afterCaret}
        {placeholderText !== '' ? (
          <Text color="gray">{placeholderText}</Text>
        ) : null}
      </Text>
    </Box>
  )
}
