import { Box, Text, useStdout } from 'ink'
import chalk from 'chalk'

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

/** Past assistant error line: full-width red background, same padding pattern as user blocks. */
export function PastAssistantErrorBlock(props: { readonly text: string }) {
  const { stdout } = useStdout()
  const cols = stdout.columns > 0 ? stdout.columns : 80

  return (
    <Box flexDirection="column" width={cols}>
      <Text>{fullWidthErrorBar(cols)}</Text>
      <Text>{fullWidthErrorContentLine(props.text, cols)}</Text>
      <Text>{fullWidthErrorBar(cols)}</Text>
    </Box>
  )
}
