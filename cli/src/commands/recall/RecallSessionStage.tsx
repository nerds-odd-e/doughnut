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
import { markMemoryTrackerRecalled } from './markMemoryTrackerRecalled.js'
import { JustReviewRecallCard } from './JustReviewRecallCard.js'
import { RecallMcqStage } from './RecallMcqStage.js'
import { SpellingRecallStage } from './SpellingRecallStage.js'
import { RECALL_SESSION_STOPPED_LINE } from './leaveRecallSessionCopy.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'
import { recallAnsweredLine } from './recallAnsweredScrollback.js'
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
  const cardRef = useRef<RecallCard | null>(null)
  const submittingRef = useRef(false)
  const successfulRecallsRef = useRef(0)
  const startedWithEmptyTodayRef = useRef(false)
  const activeOperationAbortRef = useRef<AbortController | null>(null)

  cardRef.current = card

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

  const onMcqSucceeded = useCallback(async () => {
    successfulRecallsRef.current += 1
    try {
      const next = await loadNextRecallCardIfAny(0)
      if (next !== null) {
        setCard(next)
        return
      }
      appendScrollbackItem(recallAnsweredLine('Correct!'))
      onSettled('')
    } catch (loadErr: unknown) {
      onSettled(userVisibleSlashCommandError(loadErr))
    }
  }, [appendScrollbackItem, onSettled])

  const onSpellingSessionComplete = useCallback(async () => {
    successfulRecallsRef.current += 1
    const spellCard = cardRef.current
    const spellTitle =
      spellCard?.variant === 'spelling-session'
        ? spellCard.payload.noteTitle
        : 'Note'
    const spellCorrectLine = `Spell correct: ${spellTitle}`
    try {
      const next = await loadNextRecallCardIfAny(0)
      if (next !== null) {
        appendScrollbackItem(recallAnsweredLine(spellCorrectLine))
        setCard(next)
        return
      }
      appendScrollbackItem(recallAnsweredLine(spellCorrectLine))
      onSettled('')
    } catch (loadErr: unknown) {
      onSettled(userVisibleSlashCommandError(loadErr))
    }
  }, [appendScrollbackItem, onSettled])

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

  const submitJustReview = useCallback(
    async (successful: boolean) => {
      const c = cardRef.current
      if (c === null || c.variant !== 'just-review' || submittingRef.current)
        return
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      submittingRef.current = true
      const p = c.payload
      try {
        await markMemoryTrackerRecalled(
          p.memoryTrackerId,
          successful,
          ac.signal
        )
      } catch (err: unknown) {
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(err))
        return
      }
      if (ac.signal.aborted) {
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(
          userVisibleSlashCommandError(
            new DOMException('Aborted', 'AbortError')
          )
        )
        return
      }
      if (!successful) {
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled('Marked as not recalled.')
        return
      }
      successfulRecallsRef.current += 1
      try {
        const next = await loadNextRecallCardIfAny(0, ac.signal)
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
          if (
            startedWithEmptyTodayRef.current &&
            successfulRecallsRef.current === 1
          ) {
            appendScrollbackItem(recallAnsweredLine(`Reviewed: ${p.noteTitle}`))
            onSettled('')
          } else {
            setUiMode('loadMore')
          }
        } else {
          appendScrollbackItem(recallAnsweredLine(`Reviewed: ${p.noteTitle}`))
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
    [appendScrollbackItem, onSettled]
  )

  const abortJustReviewInFlight = useCallback(() => {
    if (submittingRef.current) {
      activeOperationAbortRef.current?.abort()
    }
  }, [])

  const leaveJustReviewSession = useCallback(() => {
    onSettled(RECALL_SESSION_STOPPED_LINE)
  }, [onSettled])

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
          onSettled={onSettled}
          payload={card.payload}
          inputBlockedRef={submittingRef}
          onMcqSucceeded={onMcqSucceeded}
          onMcqPayloadReplace={(p) => setCard({ variant: 'mcq', payload: p })}
          onMcqSessionNotice={(line) =>
            appendScrollbackItem(recallAnsweredLine(line))
          }
        />
      </RecallSessionChrome>
    )
  }

  if (card.variant === 'spelling-session') {
    return (
      <RecallSessionChrome>
        <SpellingRecallStage
          key={card.payload.memoryTrackerId}
          onSettled={onSettled}
          payload={card.payload}
          inputBlockedRef={submittingRef}
          onSpellingSessionComplete={onSpellingSessionComplete}
        />
      </RecallSessionChrome>
    )
  }

  return (
    <RecallSessionChrome>
      <JustReviewRecallCard
        key={card.payload.memoryTrackerId}
        payload={card.payload}
        onAnswer={submitJustReview}
        onAbortInFlight={abortJustReviewInFlight}
        onLeaveRecallConfirmed={leaveJustReviewSession}
        inputBlockedRef={submittingRef}
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
