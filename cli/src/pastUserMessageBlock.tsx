import { Box, Text } from 'ink'
import chalk from 'chalk'

/** Past user line in the transcript: gray background, one padded row above and below the text. */
export function PastUserMessageBlock(props: { readonly text: string }) {
  const pad = chalk.bgGray(' ')
  const line = chalk.bgGray(chalk.white(props.text))
  return (
    <Box flexDirection="column">
      <Text>{pad}</Text>
      <Text>{line}</Text>
      <Text>{pad}</Text>
    </Box>
  )
}
