import { useCallback, useRef, useState } from 'react'
import type { MemoryTrackerLite } from 'donut-api'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import {
  fetchDueMemoryTrackerIds,
  loadRecallCardForMemoryTrackerId,
  type RecallCard,
} from './nextRecallCardLoad.js'
import { RecallSessionActiveCard } from './RecallSessionActiveCard.js'
import { RecallSessionChrome } from './RecallSessionChrome.js'
import { RecallSessionLoadMoreView } from './RecallSessionLoadMoreView.js'
import { RECALL_SESSION_STOPPED_LINE } from './leaveRecallSessionCopy.js'
import { RECALL_LOADING_NEXT_QUESTION_LABEL } from './recallBusyInputCopy.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'
import { recallAnsweredScrollbackItem } from './recallAnsweredScrollback.js'
import type { RecallPromptAnswerOutcome } from './recallPromptAnswerOutcome.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'

export function RecallSessionStage({
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  const { appendScrollbackItem } = useSessionScrollbackAppend()
  const [card, setCard] = useState<RecallCard | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [loadMoreFetching, setLoadMoreFetching] = useState(false)
  const [loadingNextQuestion, setLoadingNextQuestion] = useState(false)
  const [nextQuestionLoadKey, setNextQuestionLoadKey] = useState(0)
  const [initialResolved, setInitialResolved] = useState(false)
  const submittingRef = useRef(false)
  const sessionAnsweredCardsRef = useRef(0)
  const activeOperationAbortRef = useRef<AbortController | null>(null)
  const trackerQueueRef = useRef<MemoryTrackerLite[]>([])

  const settleSessionSummary = useCallback(() => {
    onSettled(recallSessionSummaryLine(sessionAnsweredCardsRef.current))
  }, [onSettled])

  const runInitialRecallLoad = useCallback(async (signal: AbortSignal) => {
    const lites = await fetchDueMemoryTrackerIds(0, signal)
    if (signal.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    trackerQueueRef.current = lites
    if (lites.length > 0) {
      const head = lites[0]!
      const next = await loadRecallCardForMemoryTrackerId(
        head.memoryTrackerId,
        head.spelling,
        signal
      )
      if (signal.aborted) {
        throw new DOMException('Aborted', 'AbortError')
      }
      setCard(next)
      setUiMode('card')
    } else {
      setCard(null)
      setLoadMoreFetching(false)
      setUiMode('loadMore')
    }
    return ''
  }, [])

  const onInitialFetchSuccess = useCallback(() => {
    setInitialResolved(true)
  }, [])

  const runLoadMoreRecall = useCallback(async (signal: AbortSignal) => {
    const lites = await fetchDueMemoryTrackerIds(3, signal)
    if (signal.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    trackerQueueRef.current = lites
    if (lites.length === 0) {
      return ''
    }
    const next = await loadRecallCardForMemoryTrackerId(
      lites[0]!.memoryTrackerId,
      lites[0]!.spelling,
      signal
    )
    if (signal.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    setUiMode('card')
    setCard(next)
    return ''
  }, [])

  const onLoadMoreFetchSuccess = useCallback(() => {
    setLoadMoreFetching(false)
    submittingRef.current = false
    if (trackerQueueRef.current.length === 0) {
      settleSessionSummary()
    }
  }, [settleSessionSummary])

  const handleLoadMoreAbort = useCallback(
    (message: string) => {
      setLoadMoreFetching(false)
      submittingRef.current = false
      onAbortWithError(message)
    },
    [onAbortWithError]
  )

  const runLoadNextRecallCard = useCallback(async (signal: AbortSignal) => {
    const head = trackerQueueRef.current[0]
    if (head === undefined) {
      throw new Error(
        'Recall queue unexpectedly empty while loading next card.'
      )
    }
    const next = await loadRecallCardForMemoryTrackerId(
      head.memoryTrackerId,
      head.spelling,
      signal
    )
    if (signal.aborted) {
      throw new DOMException('Aborted', 'AbortError')
    }
    setCard(next)
    return ''
  }, [])

  const onNextQuestionFetchSuccess = useCallback(() => {
    setLoadingNextQuestion(false)
  }, [])

  const handleNextQuestionAbort = useCallback(
    (message: string) => {
      setLoadingNextQuestion(false)
      onAbortWithError(message)
    },
    [onAbortWithError]
  )

  const onRecallFatalError = useCallback(
    (message: string) => {
      onAbortWithError(message)
    },
    [onAbortWithError]
  )

  const onConfirmLeaveRecall = useCallback(() => {
    onSettled(RECALL_SESSION_STOPPED_LINE)
  }, [onSettled])

  const onRecallQuestionAnswered = useCallback(
    (outcome: RecallPromptAnswerOutcome) => {
      sessionAnsweredCardsRef.current += 1

      for (const row of outcome.answeredRows) {
        appendScrollbackItem(recallAnsweredScrollbackItem(row))
      }
      trackerQueueRef.current.shift()
      const head = trackerQueueRef.current[0]
      if (head !== undefined) {
        setLoadingNextQuestion(true)
        setCard(null)
        setNextQuestionLoadKey((k) => k + 1)
        return
      }
      setLoadMoreFetching(false)
      setUiMode('loadMore')
    },
    [appendScrollbackItem]
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
    },
    [settleSessionSummary]
  )

  /** Load-more prompt: Esc = decline load more (same as n → session summary). In-flight load-more fetch uses Esc via `AsyncAssistantFetchStage`. */
  const escapeLoadMorePrompt = useCallback(() => {
    submitLoadMore(false).catch(() => undefined)
  }, [submitLoadMore])

  if (!initialResolved) {
    return (
      <RecallSessionChrome>
        <AsyncAssistantFetchStage
          spinnerLabel="Loading recall…"
          runAssistantMessage={runInitialRecallLoad}
          onFetchSuccess={onInitialFetchSuccess}
          onSettled={onSettled}
          onAbortWithError={onAbortWithError}
        />
      </RecallSessionChrome>
    )
  }

  if (uiMode === 'loadMore') {
    return (
      <RecallSessionLoadMoreView
        loadMoreFetching={loadMoreFetching}
        runLoadMoreRecall={runLoadMoreRecall}
        onLoadMoreFetchSuccess={onLoadMoreFetchSuccess}
        onSettled={onSettled}
        onAbortWithError={handleLoadMoreAbort}
        submitLoadMore={submitLoadMore}
        escapeLoadMorePrompt={escapeLoadMorePrompt}
        submittingRef={submittingRef}
      />
    )
  }

  if (loadingNextQuestion) {
    return (
      <RecallSessionChrome>
        <AsyncAssistantFetchStage
          key={nextQuestionLoadKey}
          spinnerLabel={RECALL_LOADING_NEXT_QUESTION_LABEL}
          runAssistantMessage={runLoadNextRecallCard}
          onFetchSuccess={onNextQuestionFetchSuccess}
          onSettled={onSettled}
          onAbortWithError={handleNextQuestionAbort}
        />
      </RecallSessionChrome>
    )
  }

  if (card === null) {
    return null
  }

  return (
    <RecallSessionActiveCard
      card={card}
      submittingRef={submittingRef}
      activeOperationAbortRef={activeOperationAbortRef}
      onRecallQuestionAnswered={onRecallQuestionAnswered}
      onReplaceCurrentRecallCard={setCard}
      onRecallFatalError={onRecallFatalError}
      onConfirmLeaveRecall={onConfirmLeaveRecall}
    />
  )
}
