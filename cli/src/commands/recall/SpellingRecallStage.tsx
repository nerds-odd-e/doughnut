import {
  Fragment,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import {
  fetchSpellingRecallPrompt,
  submitSpellingAnswer,
  type SpellingRecallSessionPayload,
} from './recallSpellingLoad.js'
import { normalizeSpellingLineForSubmit } from './spellingAnswerLine.js'

const STAGE_LABEL = 'Recalling'

const LEAVE_RECALL_PROMPT = 'Leave recall?'

const RECALL_SESSION_STOPPED_LINE = 'Recall session stopped.'

type LoadState =
  | { readonly status: 'loading' }
  | {
      readonly status: 'ready'
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
  const [loadState, setLoadState] = useState<LoadState>({ status: 'loading' })
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)

  const width = resolvedTerminalWidth()

  useEffect(() => {
    let cancelled = false
    const ac = new AbortController()
    ;(async () => {
      try {
        const fetched = await fetchSpellingRecallPrompt(
          payload.memoryTrackerId,
          ac.signal
        )
        if (cancelled) return
        setLoadState({
          status: 'ready',
          recallPromptId: fetched.recallPromptId,
          stemMarkdown: fetched.stemMarkdown,
        })
      } catch (err: unknown) {
        if (cancelled || ac.signal.aborted) return
        onSettled(userVisibleSlashCommandError(err))
      }
    })().catch(() => undefined)
    return () => {
      cancelled = true
      ac.abort()
    }
  }, [onSettled, payload.memoryTrackerId])

  const stemRendered = useMemo(() => {
    if (loadState.status !== 'ready') return ''
    return renderMarkdownToTerminal(loadState.stemMarkdown, width)
  }, [loadState, width])
  const stemLines = useMemo(
    () => (stemRendered.length > 0 ? stemRendered.split('\n') : []),
    [stemRendered]
  )

  const runSpellSubmit = useCallback(async () => {
    if (loadState.status !== 'ready' || inputBlockedRef.current) return
    const line = normalizeSpellingLineForSubmit(bufferRef.current)
    if (line === '') return
    inputBlockedRef.current = true
    try {
      const updated = await submitSpellingAnswer(loadState.recallPromptId, line)
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
  }, [inputBlockedRef, loadState, onSettled, onSpellingSessionComplete])

  const processSpellKeyEvent = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      if (key.escape === true) {
        setShowLeaveConfirm(true)
        return
      }

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

  const spellReady = loadState.status === 'ready'

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    if (!spellReady || showLeaveConfirm) return
    setStageKeyHandler(handleSpellInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleSpellInput, spellReady, showLeaveConfirm])

  useInput(handleSpellInput, {
    isActive:
      setStageKeyHandler === undefined && spellReady && !showLeaveConfirm,
  })

  if (loadState.status === 'loading') {
    return (
      <Box flexDirection="column">
        <Text>{STAGE_LABEL}</Text>
        {payload.notebookTitle !== undefined && payload.notebookTitle !== '' ? (
          <Text>{payload.notebookTitle}</Text>
        ) : null}
        <Box>
          <Spinner label="Loading spelling question…" />
        </Box>
      </Box>
    )
  }

  if (showLeaveConfirm) {
    return (
      <YesNoStagePrompt
        prompt={LEAVE_RECALL_PROMPT}
        onAnswer={(yes) => {
          if (yes) {
            onSettled(RECALL_SESSION_STOPPED_LINE)
            return
          }
          setShowLeaveConfirm(false)
        }}
        onCancel={() => setShowLeaveConfirm(false)}
        inputBlockedRef={inputBlockedRef}
        header={
          <Fragment>
            <Text>{STAGE_LABEL}</Text>
            {payload.notebookTitle !== undefined &&
            payload.notebookTitle !== '' ? (
              <Text>{payload.notebookTitle}</Text>
            ) : null}
          </Fragment>
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
