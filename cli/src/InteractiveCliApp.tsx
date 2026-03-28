import { useState } from 'react'
import { Box, Text, useApp, useInput } from 'ink'
import { formatVersionOutput } from './commands/version.js'
import { useInteractiveCliLineBuffer } from './interactiveCliInput.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'

type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const { buffer, applyInput } = useInteractiveCliLineBuffer()

  useInput((input, key) => {
    applyInput(input, key, (line) => {
      if (line === '/exit') {
        setMessages((prev) => [
          ...prev,
          { role: 'user', text: line },
          { role: 'assistant', text: 'Bye.' },
        ])
        setTimeout(() => {
          exit()
        }, 0)
        return
      }
      if (line === '') {
        return
      }
      if (!line.startsWith('/')) {
        setMessages((prev) => [
          ...prev,
          { role: 'user', text: line },
          { role: 'assistant', text: 'Not supported' },
        ])
      }
    })
  })

  return (
    <Box flexDirection="column">
      {messages.map((item, index) =>
        item.role === 'user' ? (
          <PastUserMessageBlock key={index} text={item.text} />
        ) : (
          <Text key={index}>{item.text}</Text>
        )
      )}
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
    </Box>
  )
}
