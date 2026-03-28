import { useState } from 'react'
import { Box, Static, Text, useApp, useInput } from 'ink'
import chalk from 'chalk'
import { formatVersionOutput } from './commands/version.js'

type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const [buffer, setBuffer] = useState('')

  useInput((input, key) => {
    if (key.return) {
      const line = buffer
      setBuffer('')
      if (line === '/exit') {
        setMessages((prev) => [...prev, { role: 'user', text: line }])
        queueMicrotask(() => {
          exit()
        })
      }
      return
    }
    if (key.backspace || key.delete) {
      setBuffer((b) => b.slice(0, -1))
      return
    }
    if (input) {
      setBuffer((b) => b + input)
    }
  })

  return (
    <Box flexDirection="column">
      <Static items={messages}>
        {(item, index) => (
          <Text key={index}>
            {item.role === 'user' ? chalk.grey(item.text) : item.text}
          </Text>
        )}
      </Static>
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
    </Box>
  )
}
