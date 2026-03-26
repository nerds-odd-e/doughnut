import { Spinner, TextInput } from '@inkjs/ui'
import { Box } from 'ink'
import type { InteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import { PLACEHOLDER_BY_CONTEXT } from '../renderer.js'

type Props = {
  waitLine: InteractiveFetchWaitLine
}

export function FetchWaitDisplay({ waitLine }: Props) {
  return (
    <Box flexDirection="column">
      <Spinner label={waitLine} type="dots" />
      <TextInput
        isDisabled
        placeholder={PLACEHOLDER_BY_CONTEXT.interactiveFetchWait}
        defaultValue=""
      />
    </Box>
  )
}
