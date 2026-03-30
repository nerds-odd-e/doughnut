import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
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
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  loadRecallJustReviewPayloadIfAny,
  markJustReviewRecalled,
  type RecallSessionPayload,
} from './justReviewLoad.js'
import { RecallMcqStage } from './RecallMcqStage.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'

const STAGE_LABEL = 'Recalling'

export function RecallJustReviewStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const [payload, setPayload] = useState<RecallSessionPayload | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [initialResolved, setInitialResolved] = useState(false)
  const payloadRef = useRef<RecallSessionPayload | null>(null)
  const submittingRef = useRef(false)
  const successfulRecallsRef = useRef(0)
  const startedWithEmptyTodayRef = useRef(false)
  const activeOperationAbortRef = useRef<AbortController | null>(null)

  payloadRef.current = payload

  useEffect(() => {
    let unmounted = false
    const ac = new AbortController()
    activeOperationAbortRef.current = ac
    ;(async () => {
      try {
        const p = await loadRecallJustReviewPayloadIfAny(0, ac.signal)
        if (unmounted || ac.signal.aborted) return
        if (p !== null) {
          startedWithEmptyTodayRef.current = false
          setPayload(p)
          setUiMode('card')
        } else {
          startedWithEmptyTodayRef.current = true
          setPayload(null)
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
      const next = await loadRecallJustReviewPayloadIfAny(0)
      if (next !== null) {
        setPayload(next)
        return
      }
      onSettled('Correct!\nRecalled successfully')
    } catch (loadErr: unknown) {
      onSettled(userVisibleSlashCommandError(loadErr))
    }
  }, [onSettled])

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
        const next = await loadRecallJustReviewPayloadIfAny(3, ac.signal)
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
          setPayload(next)
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

  const submit = useCallback(
    async (successful: boolean) => {
      const p = payloadRef.current
      if (p === null || p.kind !== 'just-review' || submittingRef.current)
        return
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      submittingRef.current = true
      try {
        await markJustReviewRecalled(p.memoryTrackerId, successful, ac.signal)
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
        const next = await loadRecallJustReviewPayloadIfAny(0, ac.signal)
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
            onSettled('Recalled successfully')
          } else {
            setUiMode('loadMore')
          }
        } else {
          setPayload(next)
        }
      } catch (loadErr: unknown) {
        submittingRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [onSettled]
  )

  const cancelJustReviewCard = useCallback(() => {
    submit(false).catch(() => undefined)
  }, [submit])

  const cancelLoadMorePrompt = useCallback(() => {
    submitLoadMore(false).catch(() => undefined)
  }, [submitLoadMore])

  const abortInFlightOperation = useCallback(() => {
    activeOperationAbortRef.current?.abort()
  }, [])

  const width = resolvedTerminalWidth()

  if (!initialResolved) {
    return (
      <Box flexDirection="column">
        <RecallJustReviewEscSpinner abortRef={activeOperationAbortRef} />
        <Box>
          <Spinner label="Loading recall…" />
        </Box>
      </Box>
    )
  }

  if (uiMode === 'loadMore') {
    return (
      <YesNoStagePrompt
        key="load-more"
        prompt="Load more from next 3 days?"
        onAnswer={submitLoadMore}
        defaultAnswer={true}
        onCancel={cancelLoadMorePrompt}
        onEscapeWhileInputBlocked={abortInFlightOperation}
        inputBlockedRef={submittingRef}
        header={<Text>{STAGE_LABEL}</Text>}
      />
    )
  }

  if (payload === null) {
    return (
      <Box flexDirection="column">
        <RecallJustReviewEscSpinner abortRef={activeOperationAbortRef} />
        <Box>
          <Spinner label="Loading recall…" />
        </Box>
      </Box>
    )
  }

  if (payload.kind === 'mcq') {
    return (
      <RecallMcqStage
        key={payload.recallPromptId}
        onSettled={onSettled}
        payload={payload}
        inputBlockedRef={submittingRef}
        onMcqSucceeded={onMcqSucceeded}
      />
    )
  }

  const detailsRendered = payload.detailsMarkdown
    ? renderMarkdownToTerminal(payload.detailsMarkdown, width)
    : ''
  const detailLines =
    detailsRendered.length > 0 ? detailsRendered.split('\n') : []

  return (
    <YesNoStagePrompt
      key={payload.memoryTrackerId}
      prompt="Yes, I remember?"
      onAnswer={submit}
      onCancel={cancelJustReviewCard}
      onEscapeWhileInputBlocked={abortInFlightOperation}
      inputBlockedRef={submittingRef}
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

function RecallJustReviewEscSpinner({
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
