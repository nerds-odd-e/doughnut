import { Box, Text, useStdout } from 'ink'
import chalk from 'chalk'

function fullWidthGrayBar(cols: number): string {
  return chalk.bgGray(' '.repeat(cols))
}

/** One left gray space + white text + gray fill to `cols` terminal columns. */
function fullWidthUserContentLine(text: string, cols: number): string {
  const innerCols = Math.max(0, cols - 1)
  const chars = [...text]
  const visible = chars.slice(0, innerCols).join('')
  const rightPad = innerCols - visible.length
  return (
    chalk.bgGray(' ') +
    chalk.bgGray(chalk.white(visible)) +
    chalk.bgGray(' '.repeat(Math.max(0, rightPad)))
  )
}

/** Past user line in the transcript: full-width gray background, 1 char padding left before text. */
export function PastUserMessageBlock(props: { readonly text: string }) {
  const { stdout } = useStdout()
  const cols = stdout.columns > 0 ? stdout.columns : 80

  return (
    <Box flexDirection="column" width={cols}>
      <Text>{fullWidthGrayBar(cols)}</Text>
      <Text>{fullWidthUserContentLine(props.text, cols)}</Text>
      <Text>{fullWidthGrayBar(cols)}</Text>
    </Box>
  )
}
