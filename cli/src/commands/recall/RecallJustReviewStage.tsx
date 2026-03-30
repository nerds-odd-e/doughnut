import { useCallback, useEffect, useRef, useState } from 'react'
import { Box, Text } from 'ink'
import { Spinner } from '@inkjs/ui'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  loadRecallJustReviewPayloadIfAny,
  markJustReviewRecalled,
  type RecallJustReviewPayload,
} from './justReviewLoad.js'
import { recallSessionSummaryLine } from './recallSessionSummary.js'

const STAGE_LABEL = 'Recalling'

export function RecallJustReviewStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const [payload, setPayload] = useState<RecallJustReviewPayload | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [initialResolved, setInitialResolved] = useState(false)
  const payloadRef = useRef<RecallJustReviewPayload | null>(null)
  const submittingRef = useRef(false)
  const successfulRecallsRef = useRef(0)
  const startedWithEmptyTodayRef = useRef(false)

  payloadRef.current = payload

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      try {
        const p = await loadRecallJustReviewPayloadIfAny(0)
        if (cancelled) return
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
        if (!cancelled) onSettled(userVisibleSlashCommandError(err))
      }
    }
    run().catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [onSettled])

  const settleSessionSummary = useCallback(() => {
    onSettled(recallSessionSummaryLine(successfulRecallsRef.current))
  }, [onSettled])

  const submitLoadMore = useCallback(
    async (accept: boolean) => {
      if (submittingRef.current) return
      submittingRef.current = true
      try {
        if (!accept) {
          submittingRef.current = false
          settleSessionSummary()
          return
        }
        const next = await loadRecallJustReviewPayloadIfAny(3)
        submittingRef.current = false
        if (next === null) {
          settleSessionSummary()
        } else {
          setUiMode('card')
          setPayload(next)
        }
      } catch (loadErr: unknown) {
        submittingRef.current = false
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [onSettled, settleSessionSummary]
  )

  const submit = useCallback(
    async (successful: boolean) => {
      const p = payloadRef.current
      if (p === null || submittingRef.current) return
      submittingRef.current = true
      try {
        await markJustReviewRecalled(p.memoryTrackerId, successful)
      } catch (err: unknown) {
        submittingRef.current = false
        onSettled(userVisibleSlashCommandError(err))
        return
      }
      if (!successful) {
        submittingRef.current = false
        onSettled('Marked as not recalled.')
        return
      }
      successfulRecallsRef.current += 1
      try {
        const next = await loadRecallJustReviewPayloadIfAny(0)
        submittingRef.current = false
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

  const width = resolvedTerminalWidth()

  if (!initialResolved) {
    return (
      <Box>
        <Spinner label="Loading recall…" />
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
        inputBlockedRef={submittingRef}
        header={<Text>{STAGE_LABEL}</Text>}
      />
    )
  }

  if (payload === null) {
    return (
      <Box>
        <Spinner label="Loading recall…" />
      </Box>
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
