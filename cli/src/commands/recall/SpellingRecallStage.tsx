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
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { markMemoryTrackerRecalled } from './markMemoryTrackerRecalled.js'
import {
  fetchSpellingRecallPrompt,
  submitSpellingAnswer,
  type SpellingRecallSessionPayload,
} from './recallSpellingLoad.js'

const STAGE_LABEL = 'Recalling'

type SpellPhaseState = {
  readonly recallPromptId: number
  readonly stemMarkdown: string
}

export function SpellingRecallStage({
  onSettled,
  payload,
  inputBlockedRef,
  onSpellingSessionComplete,
}: InteractiveSlashCommandStageProps & {
  readonly payload: SpellingRecallSessionPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly onSpellingSessionComplete: () => void | Promise<void>
}) {
  const [spellPhase, setSpellPhase] = useState<SpellPhaseState | null>(null)
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')

  const width = resolvedTerminalWidth()
  const detailsRendered = payload.detailsMarkdown
    ? renderMarkdownToTerminal(payload.detailsMarkdown, width)
    : ''
  const detailLines =
    detailsRendered.length > 0 ? detailsRendered.split('\n') : []

  const stemRendered = useMemo(
    () =>
      spellPhase
        ? renderMarkdownToTerminal(spellPhase.stemMarkdown, width)
        : '',
    [spellPhase, width]
  )
  const stemLines = useMemo(
    () => (stemRendered.length > 0 ? stemRendered.split('\n') : []),
    [stemRendered]
  )

  const runSpellSubmit = useCallback(async () => {
    if (spellPhase === null || inputBlockedRef.current) return
    const line = bufferRef.current
      .replace(/\r\n/g, ' ')
      .replace(/\n/g, ' ')
      .trim()
    if (line === '') return
    inputBlockedRef.current = true
    try {
      const updated = await submitSpellingAnswer(
        spellPhase.recallPromptId,
        line
      )
      const correct = updated.answer?.correct === true
      if (!correct) {
        onSettled('Incorrect.')
        return
      }
      await onSpellingSessionComplete()
    } catch (err: unknown) {
      onSettled(userVisibleSlashCommandError(err))
    } finally {
      inputBlockedRef.current = false
    }
  }, [inputBlockedRef, onSettled, onSpellingSessionComplete, spellPhase])

  const processSpellKeyEvent = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      if (key.return || input === '\n' || input === '\r') {
        runSpellSubmit().catch(() => undefined)
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
    [inputBlockedRef, runSpellSubmit]
  )

  const handleSpellInput = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      if (input.includes('\r') || input.includes('\n')) {
        const returnKey = { return: true } as Key
        const emptyKey = {} as Key
        for (const ch of input) {
          if (ch === '\r' || ch === '\n') {
            processSpellKeyEvent('\r', returnKey)
          } else if (!(key.ctrl || key.meta)) {
            processSpellKeyEvent(ch, emptyKey)
          }
        }
        return
      }

      processSpellKeyEvent(input, key)
    },
    [inputBlockedRef, processSpellKeyEvent]
  )

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    if (spellPhase === null) return
    setStageKeyHandler(handleSpellInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleSpellInput, spellPhase])

  useInput(handleSpellInput, {
    isActive: setStageKeyHandler === undefined && spellPhase !== null,
  })

  const onRememberAnswer = useCallback(
    async (successful: boolean) => {
      if (inputBlockedRef.current) return
      const ac = new AbortController()
      inputBlockedRef.current = true
      try {
        if (!successful) {
          await markMemoryTrackerRecalled(
            payload.memoryTrackerId,
            false,
            ac.signal
          )
          onSettled('Marked as not recalled.')
          return
        }
        const fetched = await fetchSpellingRecallPrompt(
          payload.memoryTrackerId,
          ac.signal
        )
        bufferRef.current = ''
        setBuffer('')
        setSpellPhase(fetched)
      } catch (err: unknown) {
        onSettled(userVisibleSlashCommandError(err))
      } finally {
        inputBlockedRef.current = false
      }
    },
    [inputBlockedRef, onSettled, payload.memoryTrackerId]
  )

  const escapeRemember = useCallback(() => {
    if (inputBlockedRef.current) return
    onRememberAnswer(false).catch(() => undefined)
  }, [inputBlockedRef, onRememberAnswer])

  if (spellPhase === null) {
    return (
      <YesNoStagePrompt
        key={payload.memoryTrackerId}
        prompt="Yes, I remember?"
        onAnswer={onRememberAnswer}
        onCancel={escapeRemember}
        inputBlockedRef={inputBlockedRef}
        header={
          <>
            <Text>{STAGE_LABEL}</Text>
            {payload.notebookTitle !== undefined &&
            payload.notebookTitle !== '' ? (
              <Text>{payload.notebookTitle}</Text>
            ) : null}
          </>
        }
        belowBuffer={
          <>
            <Text>{payload.noteTitle}</Text>
            {detailLines.map((line, i) => (
              <Text key={i}>{line.length > 0 ? line : ' '}</Text>
            ))}
          </>
        }
      />
    )
  }

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
