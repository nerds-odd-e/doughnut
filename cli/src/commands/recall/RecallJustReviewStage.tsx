import {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import {
  loadRecallJustReviewPayload,
  loadRecallJustReviewPayloadIfAny,
  markJustReviewRecalled,
  type RecallJustReviewPayload,
} from './justReviewLoad.js'

const STAGE_LABEL = 'Recalling'

function sessionSummaryLine(successfulRecalls: number): string {
  return successfulRecalls === 1
    ? 'Recalled 1 note'
    : `Recalled ${successfulRecalls} notes`
}

function normalizeCommittedLine(s: string): string {
  return s.replace(/\r\n/g, ' ').replace(/\n/g, ' ').trim()
}

export function RecallJustReviewStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [payload, setPayload] = useState<RecallJustReviewPayload | null>(null)
  const [uiMode, setUiMode] = useState<'card' | 'loadMore'>('card')
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const payloadRef = useRef<RecallJustReviewPayload | null>(null)
  const submittingRef = useRef(false)
  const successfulRecallsRef = useRef(0)

  payloadRef.current = payload

  useEffect(() => {
    bufferRef.current = buffer
  }, [buffer])

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      try {
        const p = await loadRecallJustReviewPayload()
        if (!cancelled) setPayload(p)
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
    onSettled(sessionSummaryLine(successfulRecallsRef.current))
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
          bufferRef.current = ''
          setBuffer('')
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
          bufferRef.current = ''
          setBuffer('')
          setUiMode('loadMore')
        } else {
          bufferRef.current = ''
          setBuffer('')
          setPayload(next)
        }
      } catch (loadErr: unknown) {
        submittingRef.current = false
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [onSettled]
  )

  const handleInput = useCallback(
    (input: string, key: Key) => {
      if (payload === null) return
      if (submittingRef.current) return

      const tryCommitReview = () => {
        const line = normalizeCommittedLine(bufferRef.current)
        if (line === 'y' || line === 'Y') {
          submit(true).catch(() => undefined)
          return
        }
        if (line === 'n' || line === 'N') {
          submit(false).catch(() => undefined)
          return
        }
      }

      const tryCommitLoadMore = () => {
        const line = normalizeCommittedLine(bufferRef.current)
        if (line === 'y' || line === 'Y') {
          submitLoadMore(true).catch(() => undefined)
          return
        }
        if (line === 'n' || line === 'N') {
          submitLoadMore(false).catch(() => undefined)
          return
        }
      }

      const tryCommit =
        uiMode === 'loadMore' ? tryCommitLoadMore : tryCommitReview

      /** PTY often sends `y\r` in one chunk; Ink parses it as one keypress with no `return` flag. */
      if (input.includes('\r') || input.includes('\n')) {
        for (const ch of input) {
          if (ch === '\r' || ch === '\n') tryCommit()
          else if (!(key.ctrl || key.meta)) {
            const next = bufferRef.current + ch
            bufferRef.current = next
            setBuffer(next)
          }
        }
        return
      }

      if (key.return || input === '\n' || input === '\r') {
        tryCommit()
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
    [payload, uiMode, submit, submitLoadMore]
  )

  useLayoutEffect(() => {
    if (setStageKeyHandler === undefined) return
    setStageKeyHandler(handleInput)
    return () => {
      setStageKeyHandler(null)
    }
  }, [setStageKeyHandler, handleInput])

  useInput(handleInput, {
    isActive: setStageKeyHandler === undefined,
  })

  const width = resolvedTerminalWidth()

  if (payload === null) {
    return (
      <Box>
        <Spinner label="Loading recall…" />
      </Box>
    )
  }

  if (uiMode === 'loadMore') {
    return (
      <Box flexDirection="column">
        <Text>{STAGE_LABEL}</Text>
        <Text>
          {'> '}
          {buffer}
          <Text inverse> </Text>
        </Text>
        <Text>Load more from next 3 days? (y/n)</Text>
      </Box>
    )
  }

  const detailsRendered = payload.detailsMarkdown
    ? renderMarkdownToTerminal(payload.detailsMarkdown, width)
    : ''
  const detailLines =
    detailsRendered.length > 0 ? detailsRendered.split('\n') : []

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
      <Text>{payload.noteTitle}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>Yes, I remember? (y/n)</Text>
    </Box>
  )
}
