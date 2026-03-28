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
    const commitLine = () => {
      const line = bufferRef.current
      bufferRef.current = ''
      setBuffer('')
      if (line === '/exit') {
        setMessages((prev) => [...prev, { role: 'user', text: line }])
        setTimeout(() => {
          exit()
        }, 0)
      }
    }

    if (key.backspace || key.delete) {
      const next = bufferRef.current.slice(0, -1)
      bufferRef.current = next
      setBuffer(next)
      return
    }

    for (let i = 0; i < input.length; i++) {
      const ch = input[i]!
      if (ch === '\r') {
        commitLine()
        if (input[i + 1] === '\n') i++
        continue
      }
      if (ch === '\n') {
        commitLine()
        continue
      }
      const next = bufferRef.current + ch
      bufferRef.current = next
      setBuffer(next)
    }

    if (key.return) {
      commitLine()
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
