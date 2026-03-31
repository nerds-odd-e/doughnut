import {
  Fragment,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
  type ReactElement,
} from 'react'
import type { Key } from 'ink'
import { Box, Text, useInput } from 'ink'
import { Spinner } from '@inkjs/ui'
import {
  MemoryTrackerController,
  RecallPromptController,
  type RecallPrompt,
} from 'doughnut-api'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { SetStageKeyHandlerContext } from '../accessToken/stageKeyForwardContext.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import { normalizeSpellingLineForSubmit } from './spellingAnswerLine.js'
import type { SpellingRecallSessionPayload } from './nextRecallCardLoad.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import {
  RecallAnsweredBlockShell,
  recallAnsweredBreadcrumbText,
  recallAnsweredMarkdownToDisplayLines,
  recallAnsweredQuizOutcomeInk,
} from './recallAnsweredInkShared.js'

async function fetchSpellingRecallPrompt(
  memoryTrackerId: number,
  signal?: AbortSignal
): Promise<{ readonly recallPromptId: number; readonly stemMarkdown: string }> {
  const prompt = await runDefaultBackendJson<RecallPrompt>(() =>
    MemoryTrackerController.askAQuestion({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )
  if (prompt.questionType !== 'SPELLING') {
    throw new Error('Expected a spelling recall prompt from the server.')
  }
  const recallPromptId = prompt.id
  if (recallPromptId === undefined) {
    throw new Error('Spelling recall prompt has no id.')
  }
  return {
    recallPromptId,
    stemMarkdown: prompt.spellingQuestion?.stem ?? '',
  }
}

async function submitSpellingAnswer(
  recallPromptId: number,
  spellingAnswer: string,
  signal?: AbortSignal
): Promise<RecallPrompt> {
  return runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body: { spellingAnswer },
      ...doughnutSdkOptions(signal),
    })
  )
}

function recallAnsweredSpellingInk(args: {
  readonly breadcrumbTitles: readonly string[]
  readonly detailsMarkdown: string
  readonly spellingAnswerDisplay: string
  readonly correct: boolean
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = recallAnsweredBreadcrumbText(args.breadcrumbTitles)
  const detailLines = recallAnsweredMarkdownToDisplayLines(
    args.detailsMarkdown,
    width
  )
  const ans = args.spellingAnswerDisplay
  return (
    <RecallAnsweredBlockShell>
      <Text>{crumb}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>{`Your answer: ${ans}`}</Text>
      {recallAnsweredQuizOutcomeInk(args.correct)}
    </RecallAnsweredBlockShell>
  )
}

const STAGE_LABEL = 'Recalling'

type LoadState =
  | { readonly status: 'loading' }
  | {
      readonly status: 'ready'
      readonly recallPromptId: number
      readonly stemMarkdown: string
    }

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
    outcome: RecallQuestionAnswerOutcome
  ) => void | Promise<void>
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
}) {
  const [loadState, setLoadState] = useState<LoadState>({ status: 'loading' })
  const setStageKeyHandler = useContext(SetStageKeyHandlerContext)
  const [buffer, setBuffer] = useState('')
  const bufferRef = useRef('')
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)

  const width = resolvedTerminalWidth()

  useEffect(() => {
    let cancelled = false
    const ac = new AbortController()
    ;(async () => {
      try {
        const fetched = await fetchSpellingRecallPrompt(
          payload.memoryTrackerId,
          ac.signal
        )
        if (cancelled) return
        setLoadState({
          status: 'ready',
          recallPromptId: fetched.recallPromptId,
          stemMarkdown: fetched.stemMarkdown,
        })
      } catch (err: unknown) {
        if (cancelled || ac.signal.aborted) return
        onRecallFatalError(userVisibleSlashCommandError(err))
      }
    })().catch(() => undefined)
    return () => {
      cancelled = true
      ac.abort()
    }
  }, [onRecallFatalError, payload.memoryTrackerId])

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
    try {
      const updated = await submitSpellingAnswer(loadState.recallPromptId, line)
      const correct = updated.answer?.correct === true
      const spellingAnswerDisplay =
        updated.answer?.spellingAnswer?.trim() || line
      const answeredBlock = recallAnsweredSpellingInk({
        breadcrumbTitles: payload.breadcrumbTitles,
        detailsMarkdown: payload.detailsMarkdown,
        spellingAnswerDisplay,
        correct,
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
      <Box flexDirection="column">
        <Text>{STAGE_LABEL}</Text>
        {payload.notebookTitle !== undefined && payload.notebookTitle !== '' ? (
          <Text>{payload.notebookTitle}</Text>
        ) : null}
        <Box>
          <Spinner label="Loading spelling question…" />
        </Box>
      </Box>
    )
  }

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
      <Text>Spell:</Text>
    </Box>
  )
}
