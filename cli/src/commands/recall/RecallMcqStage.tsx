import {
  Fragment,
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
import {
  choiceIndexFromSelectListSubmitLine,
  handleSelectListInkKey,
} from '../../interactions/selectListInteraction.js'
import { GuidanceListInk } from '../../guidanceListWindowInk.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import { numberedMcqMarkdownLinesForTerminal } from './numberedMcqMarkdownLines.js'
import {
  contestAndRegenerateMcq,
  submitMcqAnswer,
  type RecallMcqCardPayload,
} from './recallMcqLoad.js'

const STAGE_LABEL = 'Recalling'

const MCQ_HINT =
  '↑↓ Enter or number to select; Esc asks to leave recall (y/n confirm)'

export function RecallMcqStage({
  payload,
  inputBlockedRef,
  onMcqSucceeded,
  onMcqIncorrect,
  onRecallFatalError,
  onConfirmLeaveRecall,
  onMcqPayloadReplace,
  onMcqSessionNotice,
}: {
  readonly payload: RecallMcqCardPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly onMcqSucceeded: () => void | Promise<void>
  readonly onMcqIncorrect: () => void | Promise<void>
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
  readonly onMcqPayloadReplace: (payload: RecallMcqCardPayload) => void
  readonly onMcqSessionNotice?: (line: string) => void
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const [highlightIndex, setHighlightIndex] = useState(0)
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)

  const width = resolvedTerminalWidth()
  const stemRendered = useMemo(
    () => renderMarkdownToTerminal(payload.stem, width),
    [payload.stem, width]
  )
  const stemLines = useMemo(
    () => (stemRendered.length > 0 ? stemRendered.split('\n') : []),
    [stemRendered]
  )
  const choices = payload.choices
  const listLines = useMemo(
    () => numberedMcqMarkdownLinesForTerminal(choices, width),
    [choices, width]
  )

  const runSubmit = useCallback(
    async (choiceIdx: number) => {
      if (inputBlockedRef.current) return
      inputBlockedRef.current = true
      try {
        const updated = await submitMcqAnswer(payload.recallPromptId, choiceIdx)
        const correct = updated.answer?.correct === true
        if (!correct) {
          bufferRef.current = ''
          setBuffer('')
          setHighlightIndex(0)
          await onMcqIncorrect()
          return
        }
        await onMcqSucceeded()
      } catch (err: unknown) {
        onRecallFatalError(userVisibleSlashCommandError(err))
      } finally {
        inputBlockedRef.current = false
      }
    },
    [
      inputBlockedRef,
      onMcqIncorrect,
      onMcqSucceeded,
      onRecallFatalError,
      payload.recallPromptId,
    ]
  )

  const runContest = useCallback(async () => {
    if (inputBlockedRef.current) return
    inputBlockedRef.current = true
    try {
      const result = await contestAndRegenerateMcq(
        payload.memoryTrackerId,
        payload.notebookTitle,
        payload.recallPromptId
      )
      if (result.outcome === 'replaced') {
        setHighlightIndex(0)
        onMcqPayloadReplace(result.payload)
      } else {
        onMcqSessionNotice?.(result.message)
      }
    } catch (err: unknown) {
      onRecallFatalError(userVisibleSlashCommandError(err))
    } finally {
      inputBlockedRef.current = false
    }
  }, [
    inputBlockedRef,
    onMcqPayloadReplace,
    onMcqSessionNotice,
    onRecallFatalError,
    payload.memoryTrackerId,
    payload.notebookTitle,
    payload.recallPromptId,
  ])

  const processKeyEvent = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      const clearDraft = () => {
        bufferRef.current = ''
        setBuffer('')
      }

      handleSelectListInkKey(
        input,
        key,
        bufferRef.current,
        highlightIndex,
        choices.length,
        {
          kind: 'slash-and-number-or-highlight',
          choiceCount: choices.length,
        },
        'signal-escape',
        {
          onSetHighlightIndex: setHighlightIndex,
          onSubmitHighlightIndex: (index) => {
            clearDraft()
            runSubmit(index).catch(() => undefined)
          },
          onSubmitWithLine: (line) => {
            if (line.trim() === '/contest') {
              clearDraft()
              runContest().catch(() => undefined)
              return
            }
            const idx = choiceIndexFromSelectListSubmitLine(
              line,
              choices.length
            )
            if (idx === null) return
            clearDraft()
            runSubmit(idx).catch(() => undefined)
          },
          onEscapeSignaled: () => {
            setShowLeaveConfirm(true)
          },
          onEditBackspace: () => {
            const cur = bufferRef.current
            if (cur.length === 0) return
            const next = cur.slice(0, -1)
            bufferRef.current = next
            setBuffer(next)
          },
          onEditChar: (char) => {
            const next = bufferRef.current + char
            bufferRef.current = next
            setBuffer(next)
          },
        }
      )
    },
    [choices.length, highlightIndex, inputBlockedRef, runContest, runSubmit]
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
    if (showLeaveConfirm) return
    setStageKeyHandler(handleInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleInput, showLeaveConfirm])

  useInput(handleInput, {
    isActive: setStageKeyHandler === undefined && !showLeaveConfirm,
  })

  if (showLeaveConfirm) {
    return (
      <LeaveRecallConfirmPrompt
        onConfirmLeave={onConfirmLeaveRecall}
        onDismiss={() => setShowLeaveConfirm(false)}
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
      <Box flexDirection="column">
        <GuidanceListInk
          mode="numbered"
          lines={listLines}
          highlightItemIndex={highlightIndex}
        />
      </Box>
      <Text>{MCQ_HINT}</Text>
    </Box>
  )
}
