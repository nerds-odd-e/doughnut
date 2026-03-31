import {
  Fragment,
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
import { Box, Text, useInput } from 'ink'
import {
  RecallPromptController,
  type QuestionContestResult,
  type RecallPrompt,
} from 'doughnut-api'
import {
  choiceIndexFromSelectListSubmitLine,
  handleSelectListInkKey,
} from '../../interactions/selectListInteraction.js'
import { GuidanceListInk } from '../../guidanceListWindowInk.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { renderMarkdownToTerminal } from '../../markdown.js'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import { numberedMcqMarkdownLinesForTerminal } from './numberedMcqMarkdownLines.js'
import {
  recallMcqPayloadFromRecallPrompt,
  type RecallCard,
  type RecallMcqCardPayload,
} from './nextRecallCardLoad.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import {
  RECALL_ANSWERED_BREADCRUMB_SEP,
  recallAnsweredPlainInk,
  recallAnsweredScrollbackItem,
} from './recallAnsweredScrollback.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'

const CONTEST_REJECTED_FALLBACK =
  'Contest was not accepted. Please answer the question.'

function recallAnsweredMcqInk(args: {
  readonly breadcrumbTitles: readonly string[]
  readonly stem: string
  readonly choices: readonly string[]
  readonly selectedChoiceIndex: number
  readonly updatedPrompt: RecallPrompt
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = args.breadcrumbTitles.join(RECALL_ANSWERED_BREADCRUMB_SEP)
  const correct = args.updatedPrompt.answer?.correct === true
  const fromPredefined =
    args.updatedPrompt.predefinedQuestion?.correctAnswerIndex
  const correctChoiceIndex =
    fromPredefined !== undefined && fromPredefined !== null
      ? fromPredefined
      : correct && args.updatedPrompt.answer?.choiceIndex !== undefined
        ? args.updatedPrompt.answer.choiceIndex
        : undefined

  const stemRendered = renderMarkdownToTerminal(args.stem.trim(), width)
  const stemLines =
    stemRendered.length > 0 ? stemRendered.split('\n') : ([] as string[])
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
    <Box flexDirection="column">
      <Text>{crumb}</Text>
      {stemLines.map((line, i) => (
        <Text key={`st-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      {listLines.map((line, i) => (
        <Text key={`ch-${i}`} color={choiceLineColor(line.itemIndex)}>
          {line.text}
        </Text>
      ))}
      {correct ? (
        <Text color="green">Correct!</Text>
      ) : (
        <Text color="red">Incorrect.</Text>
      )}
    </Box>
  )
}

type ContestMcqOutcome =
  | { outcome: 'replaced'; payload: RecallMcqCardPayload }
  | { outcome: 'rejected'; message: string }

/** Contest then regenerate, or rejected outcome with a user-visible message. */
export async function contestAndRegenerateMcq(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  currentRecallPromptId: number,
  breadcrumbTitles: readonly string[],
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
  const mapped = recallMcqPayloadFromRecallPrompt(
    memoryTrackerId,
    notebookTitle,
    regenerated,
    breadcrumbTitles
  )
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

const STAGE_LABEL = 'Recalling'

const MCQ_HINT =
  '↑↓ Enter or number to select; Esc asks to leave recall (y/n confirm)'

export function RecallMcqStage({
  payload,
  inputBlockedRef,
  onRecallQuestionAnswered,
  onReplaceCurrentRecallCard,
  onRecallFatalError,
  onConfirmLeaveRecall,
}: {
  readonly payload: RecallMcqCardPayload
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
          await onRecallQuestionAnswered({
            successful: false,
            answeredRows: [
              recallAnsweredMcqInk({
                breadcrumbTitles: payload.breadcrumbTitles,
                stem: payload.stem,
                choices: payload.choices,
                selectedChoiceIndex: choiceIdx,
                updatedPrompt: updated,
              }),
            ],
          })
          return
        }
        await onRecallQuestionAnswered({
          successful: true,
          answeredRows: [
            recallAnsweredMcqInk({
              breadcrumbTitles: payload.breadcrumbTitles,
              stem: payload.stem,
              choices: payload.choices,
              selectedChoiceIndex: choiceIdx,
              updatedPrompt: updated,
            }),
          ],
        })
      } catch (err: unknown) {
        onRecallFatalError(userVisibleSlashCommandError(err))
      } finally {
        inputBlockedRef.current = false
      }
    },
    [
      inputBlockedRef,
      onRecallFatalError,
      onRecallQuestionAnswered,
      payload.breadcrumbTitles,
      payload.choices,
      payload.recallPromptId,
      payload.stem,
    ]
  )

  const runContest = useCallback(async () => {
    if (inputBlockedRef.current) return
    inputBlockedRef.current = true
    try {
      const result = await contestAndRegenerateMcq(
        payload.memoryTrackerId,
        payload.notebookTitle,
        payload.recallPromptId,
        payload.breadcrumbTitles
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
    }
  }, [
    appendScrollbackItem,
    inputBlockedRef,
    onRecallFatalError,
    onReplaceCurrentRecallCard,
    payload.breadcrumbTitles,
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
