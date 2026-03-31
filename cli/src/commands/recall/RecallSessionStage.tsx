import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type MutableRefObject,
  type ReactNode,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  loadNextRecallCardIfAny,
  type RecallCard,
} from './nextRecallCardLoad.js'
import { JustReviewRecallStage } from './JustReviewRecallStage.js'
import { RecallMcqStage } from './RecallMcqStage.js'
import { SpellingRecallStage } from './SpellingRecallStage.js'
import { RECALL_SESSION_STOPPED_LINE } from './leaveRecallSessionCopy.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'
import { recallAnsweredLine } from './recallAnsweredScrollback.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'

const STAGE_LABEL = 'Recalling'

function RecallSessionChrome({ children }: { readonly children: ReactNode }) {
  return <Box flexDirection="column">{children}</Box>
}

export function RecallSessionStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const { appendScrollbackItem } = useSessionScrollbackAppend()
  const [card, setCard] = useState<RecallCard | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [initialResolved, setInitialResolved] = useState(false)
  const submittingRef = useRef(false)
  const successfulRecallsRef = useRef(0)
  /** Answers submitted this session (correct or incorrect); drives summary when wrong exhausts the queue. */
  const sessionAnsweredCardsRef = useRef(0)
  const startedWithEmptyTodayRef = useRef(false)
  const activeOperationAbortRef = useRef<AbortController | null>(null)

  useEffect(() => {
    let unmounted = false
    const ac = new AbortController()
    activeOperationAbortRef.current = ac
    ;(async () => {
      try {
        const next = await loadNextRecallCardIfAny(0, ac.signal)
        if (unmounted || ac.signal.aborted) return
        if (next !== null) {
          startedWithEmptyTodayRef.current = false
          setCard(next)
          setUiMode('card')
        } else {
          startedWithEmptyTodayRef.current = true
          setCard(null)
          setUiMode('loadMore')
        }
        setInitialResolved(true)
      } catch (err: unknown) {
        if (unmounted) return
        onSettled(userVisibleSlashCommandError(err))
      } finally {
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
      }
    })().catch(() => undefined)
    return () => {
      unmounted = true
      ac.abort()
      if (activeOperationAbortRef.current === ac) {
        activeOperationAbortRef.current = null
      }
    }
  }, [onSettled])

  const settleSessionSummary = useCallback(() => {
    onSettled(recallSessionSummaryLine(successfulRecallsRef.current))
  }, [onSettled])

  const onRecallFatalError = useCallback(
    (message: string) => {
      onSettled(message)
    },
    [onSettled]
  )

  const onConfirmLeaveRecall = useCallback(() => {
    onSettled(RECALL_SESSION_STOPPED_LINE)
  }, [onSettled])

  const onRecallQuestionAnswered = useCallback(
    async (outcome: RecallQuestionAnswerOutcome) => {
      if (outcome.successful) {
        successfulRecallsRef.current += 1
      }
      sessionAnsweredCardsRef.current += 1
      for (const line of outcome.scrollbackLines) {
        appendScrollbackItem(recallAnsweredLine(line))
      }
      try {
        const next = await loadNextRecallCardIfAny(0)
        if (next !== null) {
          setCard(next)
          return
        }
        if (outcome.successful) {
          onSettled('')
        } else {
          onSettled(recallSessionSummaryLine(sessionAnsweredCardsRef.current))
        }
      } catch (loadErr: unknown) {
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [appendScrollbackItem, onSettled]
  )

  const submitLoadMore = useCallback(
    async (accept: boolean) => {
      if (submittingRef.current) return
      submittingRef.current = true
      if (!accept) {
        submittingRef.current = false
        settleSessionSummary()
        return
      }
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      try {
        const next = await loadNextRecallCardIfAny(3, ac.signal)
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        if (ac.signal.aborted) {
          onSettled(
            userVisibleSlashCommandError(
              new DOMException('Aborted', 'AbortError')
            )
          )
          return
        }
        if (next === null) {
          settleSessionSummary()
        } else {
          setUiMode('card')
          setCard(next)
        }
      } catch (loadErr: unknown) {
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [onSettled, settleSessionSummary]
  )

  /** Load-more prompt: Esc = decline load more (same as n → session summary), or abort in-flight fetch — not LeaveRecallConfirmPrompt. */
  const escapeLoadMorePrompt = useCallback(() => {
    if (submittingRef.current) {
      activeOperationAbortRef.current?.abort()
    } else {
      submitLoadMore(false).catch(() => undefined)
    }
  }, [submitLoadMore])

  if (!initialResolved) {
    return (
      <RecallSessionChrome>
        <Box flexDirection="column">
          <RecallSessionEscSpinner abortRef={activeOperationAbortRef} />
          <Box>
            <Spinner label="Loading recall…" />
          </Box>
        </Box>
      </RecallSessionChrome>
    )
  }

  if (uiMode === 'loadMore') {
    return (
      <RecallSessionChrome>
        <YesNoStagePrompt
          key="load-more"
          prompt="Load more from next 3 days?"
          onAnswer={submitLoadMore}
          defaultAnswer={true}
          onCancel={escapeLoadMorePrompt}
          inputBlockedRef={submittingRef}
          header={<Text>{STAGE_LABEL}</Text>}
        />
      </RecallSessionChrome>
    )
  }

  if (card === null) {
    return (
      <RecallSessionChrome>
        <Box flexDirection="column">
          <RecallSessionEscSpinner abortRef={activeOperationAbortRef} />
          <Box>
            <Spinner label="Loading recall…" />
          </Box>
        </Box>
      </RecallSessionChrome>
    )
  }

  if (card.variant === 'mcq') {
    return (
      <RecallSessionChrome>
        <RecallMcqStage
          key={card.payload.recallPromptId}
          payload={card.payload}
          inputBlockedRef={submittingRef}
          onRecallQuestionAnswered={onRecallQuestionAnswered}
          onReplaceCurrentRecallCard={setCard}
          onRecallFatalError={onRecallFatalError}
          onConfirmLeaveRecall={onConfirmLeaveRecall}
        />
      </RecallSessionChrome>
    )
  }

  if (card.variant === 'spelling-session') {
    return (
      <RecallSessionChrome>
        <SpellingRecallStage
          key={card.payload.memoryTrackerId}
          payload={card.payload}
          inputBlockedRef={submittingRef}
          onRecallQuestionAnswered={onRecallQuestionAnswered}
          onRecallFatalError={onRecallFatalError}
          onConfirmLeaveRecall={onConfirmLeaveRecall}
        />
      </RecallSessionChrome>
    )
  }

  return (
    <RecallSessionChrome>
      <JustReviewRecallStage
        payload={card.payload}
        inputBlockedRef={submittingRef}
        activeOperationAbortRef={activeOperationAbortRef}
        startedWithEmptyTodayRef={startedWithEmptyTodayRef}
        successfulRecallsRef={successfulRecallsRef}
        onSettled={onSettled}
        setCard={setCard}
        setUiMode={setUiMode}
      />
    </RecallSessionChrome>
  )
}

function RecallSessionEscSpinner({
  abortRef,
}: {
  readonly abortRef: MutableRefObject<AbortController | null>
}) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const handleStageInput = useCallback(
    (input: string, key: Key) => {
      const isEscape = key.escape === true || input === '\u001b'
      if (!isEscape) return
      abortRef.current?.abort()
    },
    [abortRef]
  )

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    setStageKeyHandler(handleStageInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleStageInput])

  useInput(handleStageInput, {
    isActive: setStageKeyHandler === undefined,
  })

  return null
}
