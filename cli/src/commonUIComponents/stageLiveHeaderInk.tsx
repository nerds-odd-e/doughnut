import stringWidth from 'string-width'
import { Box, Text } from 'ink'
import chalk from 'chalk'
import { truncateToTerminalColumns } from '../terminalColumns.js'

const RULE_CHAR = '\u2500'

/** Dim rule + bold title on one gray band; separates scrollback from the current stage. */
export function StageLiveHeaderInk({
  title,
  cols,
}: {
  readonly title: string
  readonly cols: number
}) {
  const ruleLine = chalk.bgGray(chalk.white.dim(RULE_CHAR.repeat(cols)))

  const bodyBudget = Math.max(1, cols - 1)
  const body = truncateToTerminalColumns(title, bodyBudget)
  const used = 1 + stringWidth(body)
  const pad = ' '.repeat(Math.max(0, cols - used))
  const titleLine =
    chalk.bgGray(' ') + chalk.bgGray(chalk.bold.white(body)) + chalk.bgGray(pad)

  return (
    <Box flexDirection="column" width={cols}>
      <Text>{ruleLine}</Text>
      <Text>{titleLine}</Text>
    </Box>
  )
}
