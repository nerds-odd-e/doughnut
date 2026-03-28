import { useRef, useState } from 'react'
import { Box, Text, useApp, useInput } from 'ink'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'

type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')

  useInput((input, key) => {
    if (key.return) {
      const line = bufferRef.current
      bufferRef.current = ''
      setBuffer('')
      if (line === '/exit') {
        setMessages((prev) => [...prev, { role: 'user', text: line }])
        setTimeout(() => {
          exit()
        }, 0)
      }
      return
    }
    if (key.backspace || key.delete) {
      setBuffer((b) => {
        const next = b.slice(0, -1)
        bufferRef.current = next
        return next
      })
      return
    }
    if (input) {
      setBuffer((b) => {
        const next = b + input
        bufferRef.current = next
        return next
      })
    }
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
