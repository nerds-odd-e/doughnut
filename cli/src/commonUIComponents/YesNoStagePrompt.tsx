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
import { Box, Text, useInput, useStdout } from 'ink'
import { BorderedSingleLinePromptInputInk } from './borderedSingleLinePromptInputInk.js'
import { SetStageKeyHandlerContext } from './stageKeyForwardContext.js'

function normalizeCommittedLine(s: string): string {
  return s.replace(/\r\n/g, ' ').replace(/\n/g, ' ').trim()
}

function runOnAnswer(fn: (yes: boolean) => void | Promise<void>, yes: boolean) {
  Promise.resolve(fn(yes)).catch(() => undefined)
}

function runOnCancel(fn: () => void | Promise<void>) {
  Promise.resolve(fn()).catch(() => undefined)
}

function yesNoHint(defaultAnswer: boolean | undefined): string {
  if (defaultAnswer === true) return '(Y/n)'
  if (defaultAnswer === false) return '(y/N)'
  return '(y/n)'
}

export type YesNoStagePromptProps = {
  prompt: string
  onAnswer: (yes: boolean) => void | Promise<void>
  defaultAnswer?: boolean
  /** Invoked on Esc before the input-blocked guard, so it can abort in-flight work when combined with {@link inputBlockedRef}. */
  onCancel?: () => void | Promise<void>
  inputBlockedRef?: MutableRefObject<boolean>
  /** When set, bordered line shows spinner + label (no caret); keys still gated by {@link inputBlockedRef}. */
  busyLabel?: string
  header?: ReactNode
  belowBuffer?: ReactNode
}

export function YesNoStagePrompt({
  prompt,
  onAnswer,
  defaultAnswer,
  onCancel,
  inputBlockedRef,
  busyLabel,
  header,
  belowBuffer,
}: YesNoStagePromptProps) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const { stdout } = useStdout()
  const terminalColumns = stdout.columns > 0 ? stdout.columns : 80

  const onAnswerRef = useRef(onAnswer)
  onAnswerRef.current = onAnswer

  const onCancelRef = useRef(onCancel)
  onCancelRef.current = onCancel

  const handleInput = useCallback(
    (input: string, key: Key) => {
      const isEscape = key.escape === true || input === '\u001b'
      if (isEscape) {
        if (onCancelRef.current !== undefined) {
          runOnCancel(onCancelRef.current)
        }
        return
      }

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
        if (line === '' && defaultAnswer !== undefined) {
          runOnAnswer(onAnswerRef.current, defaultAnswer)
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
    [inputBlockedRef, defaultAnswer]
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
      <BorderedSingleLinePromptInputInk
        terminalColumns={terminalColumns}
        buffer={buffer}
        caretOffset={buffer.length}
        placeholder=""
        busyLabel={busyLabel}
      />
      {belowBuffer}
      <Text>
        {prompt} {yesNoHint(defaultAnswer)}
      </Text>
    </Box>
  )
}
