import { Box, Text, useStdout } from 'ink'
import chalk from 'chalk'
import { splitTextToTerminalRows } from '../terminalColumns.js'

function fullWidthErrorBar(cols: number): string {
  return chalk.bgRed(' '.repeat(cols))
}

function fullWidthErrorContentLine(text: string, cols: number): string {
  const innerCols = Math.max(0, cols - 1)
  const chars = [...text]
  const visible = chars.slice(0, innerCols).join('')
  const rightPad = innerCols - visible.length
  return (
    chalk.bgRed(' ') +
    chalk.bgRed(chalk.white(visible)) +
    chalk.bgRed(' '.repeat(Math.max(0, rightPad)))
  )
}

const MAX_ERROR_SCROLLBACK_CHARS = 24_000
const MAX_ERROR_SCROLLBACK_ROWS = 120

/** Past assistant error: full-width red background; multiple lines for tracebacks and wrapped paths. */
export function PastAssistantErrorBlock(props: { readonly text: string }) {
  const { stdout } = useStdout()
  const cols = stdout.columns > 0 ? stdout.columns : 80
  const innerCols = Math.max(0, cols - 1)
  let t = props.text
  if (t.length > MAX_ERROR_SCROLLBACK_CHARS) {
    t = `${t.slice(0, MAX_ERROR_SCROLLBACK_CHARS)}\n… (error message truncated)`
  }
  let rows = splitTextToTerminalRows(t, innerCols)
  if (rows.length > MAX_ERROR_SCROLLBACK_ROWS) {
    rows = [
      ...rows.slice(0, MAX_ERROR_SCROLLBACK_ROWS - 1),
      '… (more lines omitted)',
    ]
  }

  return (
    <Box flexDirection="column" width={cols}>
      <Text>{fullWidthErrorBar(cols)}</Text>
      {rows.map((row, i) => (
        <Text key={i}>{fullWidthErrorContentLine(row, cols)}</Text>
      ))}
      <Text>{fullWidthErrorBar(cols)}</Text>
    </Box>
  )
}
