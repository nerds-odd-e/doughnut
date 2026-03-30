import {
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  submitSpellingAnswer,
  type RecallSpellingCardPayload,
} from './recallSpellingLoad.js'

const STAGE_LABEL = 'Recalling'

export function RecallSpellingStage({
  onSettled,
  payload,
  inputBlockedRef,
  onSpellingSucceeded,
}: InteractiveSlashCommandStageProps & {
  readonly payload: RecallSpellingCardPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly onSpellingSucceeded: () => void | Promise<void>
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')

  const width = resolvedTerminalWidth()
  const stemRendered = useMemo(
    () => renderMarkdownToTerminal(payload.stemMarkdown, width),
    [payload.stemMarkdown, width]
  )
  const stemLines = useMemo(
    () => (stemRendered.length > 0 ? stemRendered.split('\n') : []),
    [stemRendered]
  )

  const runSubmit = useCallback(async () => {
    if (inputBlockedRef.current) return
    const line = bufferRef.current
      .replace(/\r\n/g, ' ')
      .replace(/\n/g, ' ')
      .trim()
    if (line === '') return
    inputBlockedRef.current = true
    try {
      const updated = await submitSpellingAnswer(payload.recallPromptId, line)
      const correct = updated.answer?.correct === true
      if (!correct) {
        onSettled('Incorrect.')
        return
      }
      await onSpellingSucceeded()
    } catch (err: unknown) {
      onSettled(userVisibleSlashCommandError(err))
    } finally {
      inputBlockedRef.current = false
    }
  }, [inputBlockedRef, onSettled, onSpellingSucceeded, payload.recallPromptId])

  const processKeyEvent = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      if (key.return || input === '\n' || input === '\r') {
        runSubmit().catch(() => undefined)
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
    [inputBlockedRef, runSubmit]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      if (input.includes('\r') || input.includes('\n')) {
        const returnKey = { return: true } as Key
        const emptyKey = {} as Key
        for (const ch of input) {
          if (ch === '\r' || ch === '\n') {
            processKeyEvent('\r', returnKey)
          } else if (!(key.ctrl || key.meta)) {
            processKeyEvent(ch, emptyKey)
          }
        }
        return
      }

      processKeyEvent(input, key)
    },
    [inputBlockedRef, processKeyEvent]
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
      <Text>{STAGE_LABEL}</Text>
      {payload.notebookTitle !== undefined && payload.notebookTitle !== '' ? (
        <Text>{payload.notebookTitle}</Text>
      ) : null}
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
      {stemLines.map((line, i) => (
        <Text key={`s-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>Spell:</Text>
    </Box>
  )
}
