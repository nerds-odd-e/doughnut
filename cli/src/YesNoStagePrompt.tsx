import {
  useCallback,
  useContext,
  useLayoutEffect,
  useRef,
  useState,
  type MutableRefObject,
  type ReactNode,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { SetStageKeyHandlerContext } from './commands/accessToken/stageKeyForwardContext.js'

function normalizeCommittedLine(s: string): string {
  return s.replace(/\r\n/g, ' ').replace(/\n/g, ' ').trim()
}

function runOnAnswer(fn: (yes: boolean) => void | Promise<void>, yes: boolean) {
  Promise.resolve(fn(yes)).catch(() => undefined)
}

export type YesNoStagePromptProps = {
  prompt: string
  onAnswer: (yes: boolean) => void | Promise<void>
  inputBlockedRef?: MutableRefObject<boolean>
  header?: ReactNode
  belowBuffer?: ReactNode
}

export function YesNoStagePrompt({
  prompt,
  onAnswer,
  inputBlockedRef,
  header,
  belowBuffer,
}: YesNoStagePromptProps) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')

  const onAnswerRef = useRef(onAnswer)
  onAnswerRef.current = onAnswer

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef?.current) return

      const tryCommit = () => {
        const line = normalizeCommittedLine(bufferRef.current)
        if (line === 'y' || line === 'Y') {
          runOnAnswer(onAnswerRef.current, true)
          return
        }
        if (line === 'n' || line === 'N') {
          runOnAnswer(onAnswerRef.current, false)
          return
        }
      }

      /** PTY often sends `y\r` in one chunk; Ink parses it as one keypress with no `return` flag. */
      if (input.includes('\r') || input.includes('\n')) {
        for (const ch of input) {
          if (ch === '\r' || ch === '\n') tryCommit()
          else if (!(key.ctrl || key.meta)) {
            const next = bufferRef.current + ch
            bufferRef.current = next
            setBuffer(next)
          }
        }
        return
      }

      if (key.return || input === '\n' || input === '\r') {
        tryCommit()
        return
      }

      if (key.backspace || key.delete) {
        const cur = bufferRef.current
        if (cur.length === 0) return
        const next = cur.slice(0, -1)
        bufferRef.current = next
        setBuffer(next)
        return
      }

      if (input === '' || key.ctrl || key.meta) return
      const piece = input.replace(/\r\n/g, ' ').replace(/\n/g, ' ')
      const next = bufferRef.current + piece
      bufferRef.current = next
      setBuffer(next)
    },
    [inputBlockedRef]
  )

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    setStageKeyHandler(handleInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleInput])

  useInput(handleInput, {
    isActive: setStageKeyHandler === undefined,
  })

  return (
    <Box flexDirection="column">
      {header}
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
      {belowBuffer}
      <Text>{prompt} (y/n)</Text>
    </Box>
  )
}
