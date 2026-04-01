import {
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
  type ReactElement,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput, useStdout } from 'ink'
import {
  RecallPromptController,
  type QuestionContestResult,
  type RecallPrompt,
} from 'doughnut-api'
import {
  choiceIndexFromSelectListSubmitLine,
  handleSelectListInkKey,
} from '../../interactions/selectListInteraction.js'
import { BorderedSingleLinePromptInputInk } from '../../commonUIComponents/borderedSingleLinePromptInputInk.js'
import { GuidanceListInk } from '../../commonUIComponents/guidanceListWindowInk.js'
import {
  inkTerminalColumns,
  resolvedTerminalWidth,
} from '../../terminalColumns.js'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'
import {
  TimedToastInk,
  useTimedToastInk,
} from '../../commonUIComponents/timedToastInk.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import {
  RECALL_BUSY_REGENERATE_QUESTION_LABEL,
  RECALL_BUSY_SUBMIT_ANSWER_LABEL,
} from './recallBusyInputCopy.js'
import { numberedMcqMarkdownLinesForTerminal } from './numberedMcqMarkdownLines.js'
import {
  recallMcqPayloadFromRecallPrompt,
  type RecallCard,
  type RecallMcqCardPayload,
} from './nextRecallCardLoad.js'
import { noteBreadcrumbTrailTitles } from './recallNoteContext.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import {
  RecallAnsweredBlockShell,
  recallAnsweredBreadcrumbText,
  recallAnsweredMarkdownToDisplayLines,
  recallAnsweredQuizOutcomeInk,
} from './recallAnsweredInkShared.js'
import {
  recallAnsweredPlainInk,
  recallAnsweredScrollbackItem,
} from './recallAnsweredScrollback.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'

const CONTEST_REJECTED_FALLBACK =
  'Contest was not accepted. Please answer the question.'

function recallAnsweredMcqInk(args: {
  readonly answeredPrompt: RecallPrompt
  readonly stem: string
  readonly choices: readonly string[]
  readonly selectedChoiceIndex: number
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = recallAnsweredBreadcrumbText(
    noteBreadcrumbTrailTitles(args.answeredPrompt.note)
  )
  const correct = args.answeredPrompt.answer?.correct === true
  const fromPredefined =
    args.answeredPrompt.predefinedQuestion?.correctAnswerIndex
  const correctChoiceIndex =
    fromPredefined !== undefined && fromPredefined !== null
      ? fromPredefined
      : correct && args.answeredPrompt.answer?.choiceIndex !== undefined
        ? args.answeredPrompt.answer.choiceIndex
        : undefined

  const stemLines = recallAnsweredMarkdownToDisplayLines(args.stem, width)
  const listLines = numberedMcqMarkdownLinesForTerminal(args.choices, width)
  const sel = args.selectedChoiceIndex

  const choiceLineColor = (itemIndex: number): undefined | 'green' | 'red' => {
    if (correctChoiceIndex !== undefined && itemIndex === correctChoiceIndex) {
      return 'green'
    }
    if (!correct && itemIndex === sel) {
      return 'red'
    }
    return undefined
  }

  return (
    <RecallAnsweredBlockShell>
      <Text>{crumb}</Text>
      {stemLines.map((line, i) => (
        <Text key={`st-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      {listLines.map((line, i) => (
        <Text key={`ch-${i}`} color={choiceLineColor(line.itemIndex)}>
          {line.text}
        </Text>
      ))}
      {recallAnsweredQuizOutcomeInk(correct)}
    </RecallAnsweredBlockShell>
  )
}

type ContestMcqOutcome =
  | { outcome: 'replaced'; payload: RecallMcqCardPayload }
  | { outcome: 'rejected'; message: string }

/** Contest then regenerate, or rejected outcome with a user-visible message. */
export async function contestAndRegenerateMcq(
  memoryTrackerId: number,
  currentRecallPromptId: number,
  signal?: AbortSignal
): Promise<ContestMcqOutcome> {
  const contestResult = await runDefaultBackendJson<QuestionContestResult>(() =>
    RecallPromptController.contest({
      path: { recallPrompt: currentRecallPromptId },
      ...doughnutSdkOptions(signal),
    })
  )
  if (contestResult.rejected === true) {
    const message = contestResult.advice?.trim() || CONTEST_REJECTED_FALLBACK
    return { outcome: 'rejected', message }
  }
  const regenerated = await runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.regenerate({
      path: { recallPrompt: currentRecallPromptId },
      body: contestResult,
      ...doughnutSdkOptions(signal),
    })
  )
  const mapped = recallMcqPayloadFromRecallPrompt(memoryTrackerId, regenerated)
  if (mapped === null) {
    throw new Error('Regenerated recall prompt is not a pending MCQ.')
  }
  return { outcome: 'replaced', payload: mapped }
}

export async function submitMcqAnswer(
  recallPromptId: number,
  choiceIndex: number,
  signal?: AbortSignal
): Promise<RecallPrompt> {
  return runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex },
      ...doughnutSdkOptions(signal),
    })
  )
}

const MCQ_HINT =
  '↑↓ Enter or number to select; Esc asks to leave recall (y/n confirm)'

function mcqInvalidChoiceHintMessage(choiceCount: number): string {
  return `Not a valid choice. Enter 1–${choiceCount}, use ↑↓, or /contest.`
}

export function RecallMcqStage({
  payload,
  choicesGuidanceRowBudget,
  inputBlockedRef,
  onRecallQuestionAnswered,
  onReplaceCurrentRecallCard,
  onRecallFatalError,
  onConfirmLeaveRecall,
}: {
  readonly payload: RecallMcqCardPayload
  readonly choicesGuidanceRowBudget: number
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly onRecallQuestionAnswered: (
    outcome: RecallQuestionAnswerOutcome
  ) => void | Promise<void>
  readonly onReplaceCurrentRecallCard: (card: RecallCard) => void
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
}) {
  const { appendScrollbackItem } = useSessionScrollbackAppend()
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const { stdout } = useStdout()
  const promptCols = inkTerminalColumns(stdout.columns)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const [highlightIndex, setHighlightIndex] = useState(0)
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const [commandLineBusyLabel, setCommandLineBusyLabel] = useState<
    string | undefined
  >(undefined)
  const { message: toastMessage, showToast, clearToast } = useTimedToastInk()

  const width = resolvedTerminalWidth()
  const stemLines = useMemo(
    () => recallAnsweredMarkdownToDisplayLines(payload.stem, width),
    [payload.stem, width]
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
      setCommandLineBusyLabel(RECALL_BUSY_SUBMIT_ANSWER_LABEL)
      try {
        const updated = await submitMcqAnswer(payload.recallPromptId, choiceIdx)
        const correct = updated.answer?.correct === true
        if (!correct) {
          bufferRef.current = ''
          setBuffer('')
          setHighlightIndex(0)
          await onRecallQuestionAnswered({
            successful: false,
            answeredRows: [
              recallAnsweredMcqInk({
                answeredPrompt: updated,
                stem: payload.stem,
                choices: payload.choices,
                selectedChoiceIndex: choiceIdx,
              }),
            ],
          })
          return
        }
        await onRecallQuestionAnswered({
          successful: true,
          answeredRows: [
            recallAnsweredMcqInk({
              answeredPrompt: updated,
              stem: payload.stem,
              choices: payload.choices,
              selectedChoiceIndex: choiceIdx,
            }),
          ],
        })
      } catch (err: unknown) {
        onRecallFatalError(userVisibleSlashCommandError(err))
      } finally {
        inputBlockedRef.current = false
        setCommandLineBusyLabel(undefined)
      }
    },
    [
      inputBlockedRef,
      onRecallFatalError,
      onRecallQuestionAnswered,
      payload.choices,
      payload.recallPromptId,
      payload.stem,
    ]
  )

  const runContest = useCallback(async () => {
    if (inputBlockedRef.current) return
    inputBlockedRef.current = true
    setCommandLineBusyLabel(RECALL_BUSY_REGENERATE_QUESTION_LABEL)
    try {
      const result = await contestAndRegenerateMcq(
        payload.memoryTrackerId,
        payload.recallPromptId
      )
      if (result.outcome === 'replaced') {
        setHighlightIndex(0)
        onReplaceCurrentRecallCard({
          variant: 'mcq',
          payload: result.payload,
        })
      } else {
        appendScrollbackItem(
          recallAnsweredScrollbackItem(recallAnsweredPlainInk(result.message))
        )
      }
    } catch (err: unknown) {
      onRecallFatalError(userVisibleSlashCommandError(err))
    } finally {
      inputBlockedRef.current = false
      setCommandLineBusyLabel(undefined)
    }
  }, [
    appendScrollbackItem,
    inputBlockedRef,
    onRecallFatalError,
    onReplaceCurrentRecallCard,
    payload.memoryTrackerId,
    payload.recallPromptId,
  ])

  const processKeyEvent = useCallback(
    (input: string, key: Key) => {
      if (inputBlockedRef.current) return

      const clearDraft = () => {
        clearToast()
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
          onSetHighlightIndex: (index) => {
            clearToast()
            setHighlightIndex(index)
          },
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
          onInvalidSelectListSubmitLine: () => {
            showToast(mcqInvalidChoiceHintMessage(choices.length))
          },
          onEscapeSignaled: () => {
            setShowLeaveConfirm(true)
          },
          onEditBackspace: () => {
            clearToast()
            const cur = bufferRef.current
            if (cur.length === 0) return
            const next = cur.slice(0, -1)
            bufferRef.current = next
            setBuffer(next)
          },
          onEditChar: (char) => {
            clearToast()
            const next = bufferRef.current + char
            bufferRef.current = next
            setBuffer(next)
          },
        }
      )
    },
    [
      choices.length,
      clearToast,
      highlightIndex,
      inputBlockedRef,
      runContest,
      runSubmit,
      showToast,
    ]
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
      />
    )
  }

  return (
    <Box flexDirection="column">
      <BorderedSingleLinePromptInputInk
        terminalColumns={promptCols}
        buffer={buffer}
        caretOffset={buffer.length}
        placeholder={MCQ_HINT}
        busyLabel={commandLineBusyLabel}
      />
      {stemLines.map((line, i) => (
        <Text key={`s-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Box flexDirection="column">
        <GuidanceListInk
          mode="numbered"
          lines={listLines}
          highlightItemIndex={highlightIndex}
          rowBudget={choicesGuidanceRowBudget}
        />
        <TimedToastInk message={toastMessage} />
      </Box>
    </Box>
  )
}
