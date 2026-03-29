import { useCallback } from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { useInteractiveCliLineBuffer } from './interactiveCliInput.js'

const DEFAULT_INTERACTIVE_GUIDANCE = '/ commands'

export function MainInteractivePrompt({
  onCommittedLine,
}: {
  readonly onCommittedLine: (line: string) => void
}) {
  const { buffer, applyInput } = useInteractiveCliLineBuffer()

  const handleInput = useCallback(
    (input: string, key: Key) => {
      applyInput(input, key, (line) => {
        if (line === '') {
          return
        }
        onCommittedLine(line)
      })
    },
    [applyInput, onCommittedLine]
  )

  useInput(handleInput)

  return (
    <Box flexDirection="column">
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
      <Text>{DEFAULT_INTERACTIVE_GUIDANCE}</Text>
    </Box>
  )
}
