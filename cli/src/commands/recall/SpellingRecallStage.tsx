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
import { Box, Text, useInput, useStdout } from 'ink'
import { Spinner } from '@inkjs/ui'
import { BorderedSingleLinePromptInputInk } from '../../commonUIComponents/borderedSingleLinePromptInputInk.js'
import { renderMarkdownToTerminal } from '../../markdown.js'
import {
  inkTerminalColumns,
  resolvedTerminalWidth,
} from '../../terminalColumns.js'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import { RECALL_BUSY_SUBMIT_ANSWER_LABEL } from './recallBusyInputCopy.js'
import { normalizeSpellingLineForSubmit } from './spellingAnswerLine.js'
import type { SpellingRecallSessionPayload } from './nextRecallCardLoad.js'
import type { RecallPromptAnswerOutcome } from './recallPromptAnswerOutcome.js'
import {
  recallAnsweredSpellingInk,
  submitSpellingAnswer,
  useSpellingRecallPromptLoad,
} from './spellingRecallStageSupport.js'

const SPELL_INPUT_PLACEHOLDER = 'Type answer, Enter to submit'

export function SpellingRecallStage({
  payload,
  inputBlockedRef,
  onRecallQuestionAnswered,
  onRecallFatalError,
  onConfirmLeaveRecall,
}: {
  readonly payload: SpellingRecallSessionPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly onRecallQuestionAnswered: (
    outcome: RecallPromptAnswerOutcome
  ) => void | Promise<void>
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
}) {
  const loadState = useSpellingRecallPromptLoad(payload, onRecallFatalError)
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const { stdout } = useStdout()
  const promptCols = inkTerminalColumns(stdout.columns)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const [spellBusyLabel, setSpellBusyLabel] = useState<string | undefined>(
    undefined
  )

  const width = resolvedTerminalWidth()
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
    setSpellBusyLabel(RECALL_BUSY_SUBMIT_ANSWER_LABEL)
    try {
      const updated = await submitSpellingAnswer(loadState.recallPromptId, line)
      const correct = updated.answer?.correct === true
      const spellingAnswerDisplay =
        updated.answer?.spellingAnswer?.trim() || line
      const answeredBlock = recallAnsweredSpellingInk({
        answeredPrompt: updated,
        contentMarkdownFallback: payload.contentMarkdown,
        spellingAnswerDisplay,
        notebookName: payload.notebookName,
      })
      if (!correct) {
        bufferRef.current = ''
        setBuffer('')
        await onRecallQuestionAnswered({
          successful: false,
          answeredRows: [answeredBlock],
        })
        return
      }
      await onRecallQuestionAnswered({
        successful: true,
        answeredRows: [answeredBlock],
      })
    } catch (err: unknown) {
      onRecallFatalError(userVisibleSlashCommandError(err))
    } finally {
      inputBlockedRef.current = false
      setSpellBusyLabel(undefined)
    }
  }, [
    inputBlockedRef,
    loadState,
    onRecallFatalError,
    onRecallQuestionAnswered,
    payload,
  ])

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
      <Box>
        <Spinner label="Loading spelling question…" />
      </Box>
    )
  }

  if (showLeaveConfirm) {
    return (
      <LeaveRecallConfirmPrompt
        onConfirmLeave={onConfirmLeaveRecall}
        onDismiss={() => setShowLeaveConfirm(false)}
        inputBlockedRef={inputBlockedRef}
      />
    )
  }

  return (
    <Box flexDirection="column">
      <BorderedSingleLinePromptInputInk
        terminalColumns={promptCols}
        buffer={buffer}
        caretOffset={buffer.length}
        placeholder={SPELL_INPUT_PLACEHOLDER}
        busyLabel={spellBusyLabel}
      />
      {stemLines.map((line, i) => (
        <Text key={`s-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
    </Box>
  )
}
