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
import type { MemoryTrackerLite } from 'doughnut-api'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../../commonUIComponents/stageKeyForwardContext.js'
import { YesNoStagePrompt } from '../../commonUIComponents/YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  fetchDueMemoryTrackerIds,
  fetchShuffledDueMemoryTrackerIds,
  loadRecallCardForMemoryTrackerId,
  type RecallCard,
} from './nextRecallCardLoad.js'
import { JustReviewRecallStage } from './JustReviewRecallStage.js'
import { RecallMcqStage } from './RecallMcqStage.js'
import { SpellingRecallStage } from './SpellingRecallStage.js'
import { RECALL_SESSION_STOPPED_LINE } from './leaveRecallSessionCopy.js'
import { RECALL_LOADING_NEXT_QUESTION_LABEL } from './recallBusyInputCopy.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'
import { recallAnsweredScrollbackItem } from './recallAnsweredScrollback.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'

const RECALL_NOTEBOOK_LINE_EMOJI = '📓'

function RecallSessionChrome({
  notebookTitle,
  children,
}: {
  readonly notebookTitle?: string
  readonly children: ReactNode
}) {
  const trimmed = notebookTitle?.trim()
  const showNotebook = trimmed !== undefined && trimmed.length > 0
  return (
    <Box flexDirection="column">
      {showNotebook ? (
        <Text>
          {RECALL_NOTEBOOK_LINE_EMOJI} {trimmed}
        </Text>
      ) : null}
      {children}
    </Box>
  )
}

export function RecallSessionStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const { appendScrollbackItem } = useSessionScrollbackAppend()
  const [card, setCard] = useState<RecallCard | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [loadMoreFetching, setLoadMoreFetching] = useState(false)
  const [loadingNextQuestion, setLoadingNextQuestion] = useState(false)
  const [initialResolved, setInitialResolved] = useState(false)
  const submittingRef = useRef(false)
  const sessionAnsweredCardsRef = useRef(0)
  const startedWithEmptyTodayRef = useRef(false)
  const activeOperationAbortRef = useRef<AbortController | null>(null)
  const currentRecallCardRef = useRef<RecallCard | null>(null)
  const trackerQueueRef = useRef<MemoryTrackerLite[]>([])
  currentRecallCardRef.current = card

  useEffect(() => {
    let unmounted = false
    const ac = new AbortController()
    activeOperationAbortRef.current = ac
    ;(async () => {
      try {
        const lites = await fetchDueMemoryTrackerIds(0, ac.signal)
        if (unmounted || ac.signal.aborted) return
        trackerQueueRef.current = lites
        if (lites.length > 0) {
          startedWithEmptyTodayRef.current = false
          const head = lites[0]!
          const next = await loadRecallCardForMemoryTrackerId(
            head.memoryTrackerId,
            head.spelling,
            ac.signal
          )
          if (unmounted || ac.signal.aborted) return
          setCard(next)
          setUiMode('card')
        } else {
          startedWithEmptyTodayRef.current = true
          setCard(null)
          setLoadMoreFetching(false)
          setUiMode('loadMore')
        }
        setInitialResolved(true)
      } catch (err: unknown) {
        if (unmounted) return
        onSettled(userVisibleSlashCommandError(err), true)
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
    onSettled(recallSessionSummaryLine(sessionAnsweredCardsRef.current))
  }, [onSettled])

  const onRecallFatalError = useCallback(
    (message: string) => {
      onSettled(message, true)
    },
    [onSettled]
  )

  const onConfirmLeaveRecall = useCallback(() => {
    onSettled(RECALL_SESSION_STOPPED_LINE)
  }, [onSettled])

  const onRecallQuestionAnswered = useCallback(
    async (outcome: RecallQuestionAnswerOutcome) => {
      sessionAnsweredCardsRef.current += 1

      for (const row of outcome.answeredRows) {
        appendScrollbackItem(recallAnsweredScrollbackItem(row))
      }
      try {
        trackerQueueRef.current.shift()
        const head = trackerQueueRef.current[0]
        if (head !== undefined) {
          setLoadingNextQuestion(true)
          setCard(null)
          const ac = new AbortController()
          activeOperationAbortRef.current = ac
          try {
            const next = await loadRecallCardForMemoryTrackerId(
              head.memoryTrackerId,
              head.spelling,
              ac.signal
            )
            if (ac.signal.aborted) {
              onSettled(
                userVisibleSlashCommandError(
                  new DOMException('Aborted', 'AbortError')
                ),
                true
              )
              return
            }
            setCard(next)
          } catch (loadErr: unknown) {
            if (!ac.signal.aborted) {
              onSettled(userVisibleSlashCommandError(loadErr), true)
            } else {
              onSettled(
                userVisibleSlashCommandError(
                  new DOMException('Aborted', 'AbortError')
                ),
                true
              )
            }
          } finally {
            setLoadingNextQuestion(false)
            if (activeOperationAbortRef.current === ac) {
              activeOperationAbortRef.current = null
            }
          }
          return
        }
        setLoadMoreFetching(false)
        setUiMode('loadMore')
      } catch (loadErr: unknown) {
        onSettled(userVisibleSlashCommandError(loadErr), true)
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
      setLoadMoreFetching(true)
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      try {
        const lites = await fetchShuffledDueMemoryTrackerIds(3, ac.signal)
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        if (ac.signal.aborted) {
          onSettled(
            userVisibleSlashCommandError(
              new DOMException('Aborted', 'AbortError')
            ),
            true
          )
          return
        }
        trackerQueueRef.current = lites
        const next =
          lites.length > 0
            ? await loadRecallCardForMemoryTrackerId(
                lites[0]!.memoryTrackerId,
                lites[0]!.spelling,
                ac.signal
              )
            : null
        if (ac.signal.aborted) {
          onSettled(
            userVisibleSlashCommandError(
              new DOMException('Aborted', 'AbortError')
            ),
            true
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
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(loadErr), true)
      } finally {
        submittingRef.current = false
        setLoadMoreFetching(false)
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
    if (loadMoreFetching) {
      return (
        <RecallSessionChrome>
          <Box flexDirection="column">
            <RecallSessionEscSpinner abortRef={activeOperationAbortRef} />
            <Box>
              <Spinner label="Loading more…" />
            </Box>
          </Box>
        </RecallSessionChrome>
      )
    }
    return (
      <RecallSessionChrome>
        <YesNoStagePrompt
          key="load-more"
          prompt="Load more from next 3 days?"
          onAnswer={submitLoadMore}
          defaultAnswer={true}
          onCancel={escapeLoadMorePrompt}
          inputBlockedRef={submittingRef}
        />
      </RecallSessionChrome>
    )
  }

  if (loadingNextQuestion) {
    return (
      <RecallSessionChrome>
        <Box flexDirection="column">
          <RecallSessionEscSpinner abortRef={activeOperationAbortRef} />
          <Box>
            <Spinner label={RECALL_LOADING_NEXT_QUESTION_LABEL} />
          </Box>
        </Box>
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
      <RecallSessionChrome notebookTitle={card.payload.notebookTitle}>
        <RecallMcqStage
          key={card.payload.recallPromptId}
          payload={card.payload}
          choicesGuidanceRowBudget={10}
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
      <RecallSessionChrome notebookTitle={card.payload.notebookTitle}>
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
    <RecallSessionChrome notebookTitle={card.payload.notebookTitle}>
      <JustReviewRecallStage
        payload={card.payload}
        inputBlockedRef={submittingRef}
        activeOperationAbortRef={activeOperationAbortRef}
        onRecallQuestionAnswered={onRecallQuestionAnswered}
        onRecallFatalError={onRecallFatalError}
        onConfirmLeaveRecall={onConfirmLeaveRecall}
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
