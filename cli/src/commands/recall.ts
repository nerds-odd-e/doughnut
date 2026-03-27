import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  type Answer,
  type AnswerDto,
  type AnswerSpellingDto,
  type DueMemoryTrackers,
  type MemoryTracker,
  type MemoryTrackerLite,
  type Note,
  type Notebook,
  type QuestionContestResult,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
  runWithDefaultBackendClient,
} from './accessToken.js'
import { isFetchAbortedByCaller, userAbortError } from '../fetchAbort.js'

/**
 * Env var: milliseconds to wait before the first `recalling` HTTP call when `recallNext` receives an
 * `AbortSignal` (interactive TTY fetch-wait). Vitest fake timers / manual Esc practice. Capped in `recallNext`.
 */
export const RECALL_LOAD_CLI_TEST_DELAY_MS_ENV =
  'DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS' as const

const RECALL_LOAD_CLI_TEST_DELAY_MAX_MS = 60_000

function parseCliTestRecallLoadDelayMs(): number {
  const raw = process.env[RECALL_LOAD_CLI_TEST_DELAY_MS_ENV]
  if (raw == null || raw === '') return 0
  const n = Number.parseInt(raw, 10)
  if (!Number.isFinite(n) || n <= 0) return 0
  return Math.min(n, RECALL_LOAD_CLI_TEST_DELAY_MAX_MS)
}

/** Only when `abortSignal` is set (interactive fetch-wait); honours abort during the delay. */
async function awaitCliTestRecallLoadDelayIfConfigured(
  abortSignal: AbortSignal | undefined
): Promise<void> {
  const ms = parseCliTestRecallLoadDelayMs()
  if (ms === 0 || abortSignal == null) return
  await new Promise<void>((resolve, reject) => {
    if (abortSignal.aborted) {
      reject(userAbortError())
      return
    }
    const t = setTimeout(() => {
      abortSignal.removeEventListener('abort', onAbort)
      resolve()
    }, ms)
    const onAbort = () => {
      clearTimeout(t)
      abortSignal.removeEventListener('abort', onAbort)
      reject(userAbortError())
    }
    abortSignal.addEventListener('abort', onAbort, { once: true })
  })
}

function dueRecallQuery(dueindays: number) {
  return {
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    dueindays,
  }
}

export type RecallNextResult =
  | { type: 'none'; message: string }
  | {
      type: 'just-review'
      memoryTrackerId: NonNullable<MemoryTrackerLite['memoryTrackerId']>
      notebookTitle: string
      title: string
      details?: string
    }
  | { type: 'question'; prompt: RecallPrompt }

export function formatRecallNotebookCurrentPromptLine(
  notebookTitle: string
): string {
  return `📓 ${notebookTitle}`
}

export function resolveRecallNotebookTitle(
  notebook?: Notebook,
  note?: Note
): string {
  return notebook?.title ?? note?.noteTopology?.notebookTitle ?? 'Notebook'
}

function isTerminalRecallQuestion(
  prompt: RecallPrompt | null | undefined
): prompt is RecallPrompt {
  if (prompt == null) return false
  if (prompt.questionType === 'MCQ' && prompt.multipleChoicesQuestion)
    return true
  return prompt.questionType === 'SPELLING'
}

export async function recallStatus(signal?: AbortSignal): Promise<string> {
  const trackers = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(0),
      ...doughnutSdkOptions(signal),
    })
  )
  const count = trackers.toRepeat?.length ?? 0
  if (count === 1) {
    return '1 note to recall today'
  }
  return `${count} notes to recall today`
}

/**
 * Fetches the next recall prompt for the session.
 * When `abortSignal` is set (interactive fetch-wait for `/recall`), HTTP calls and optional
 * {@link RECALL_LOAD_CLI_TEST_DELAY_MS_ENV} honour it so Esc can cancel before or during fetch.
 */
export async function recallNext(
  dueindays = 0,
  abortSignal?: AbortSignal
): Promise<RecallNextResult> {
  await awaitCliTestRecallLoadDelayIfConfigured(abortSignal)
  const opts = doughnutSdkOptions(abortSignal)
  const trackers = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueindays),
      ...opts,
    })
  )
  const toRepeat = trackers.toRepeat ?? []
  const first: MemoryTrackerLite | undefined = toRepeat[0]
  if (!first?.memoryTrackerId) {
    return { type: 'none', message: '0 notes to recall today' }
  }

  let prompt: RecallPrompt | null = null
  try {
    prompt = await runDefaultBackendJson<RecallPrompt | null>(() =>
      MemoryTrackerController.askAQuestion({
        path: { memoryTracker: first.memoryTrackerId },
        ...opts,
      })
    )
  } catch (e) {
    if (isFetchAbortedByCaller(e)) throw e
  }
  if (isTerminalRecallQuestion(prompt)) {
    return { type: 'question', prompt }
  }

  const trackerPayload = await runDefaultBackendJson<MemoryTracker>(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: first.memoryTrackerId },
      ...opts,
    })
  )
  const title = trackerPayload.note?.noteTopology?.title ?? 'Untitled note'
  const details = trackerPayload.note?.details
  return {
    type: 'just-review',
    memoryTrackerId: first.memoryTrackerId,
    notebookTitle: resolveRecallNotebookTitle(undefined, trackerPayload.note),
    title,
    details,
  }
}

export async function markAsRecalled(
  memoryTrackerId: number,
  successful: boolean
): Promise<void> {
  await runWithDefaultBackendClient(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
      ...doughnutSdkOptions(),
    })
  )
}

export async function answerQuiz(
  recallPromptId: number,
  choiceIndex: number,
  thinkingTimeMs?: number
): Promise<Pick<Answer, 'correct'>> {
  const body: AnswerDto = {
    choiceIndex,
    ...(thinkingTimeMs != null && { thinkingTimeMs }),
  }
  const answered = await runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body,
      ...doughnutSdkOptions(),
    })
  )
  return { correct: answered.answer?.correct ?? false }
}

export async function answerSpelling(
  recallPromptId: number,
  spellingAnswer: string,
  thinkingTimeMs?: number
): Promise<Pick<Answer, 'correct'>> {
  const body: AnswerSpellingDto = {
    spellingAnswer,
    ...(thinkingTimeMs != null && { thinkingTimeMs }),
  }
  const answered = await runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body,
      ...doughnutSdkOptions(),
    })
  )
  return { correct: answered.answer?.correct ?? false }
}

type ContestRegenerateSuccess = Extract<RecallNextResult, { type: 'question' }>

type ContestRegenerateOutcome =
  | { ok: true; result: ContestRegenerateSuccess }
  | { ok: false; message: string }

export async function contestAndRegenerate(
  recallPromptId: number,
  signal?: AbortSignal
): Promise<ContestRegenerateOutcome> {
  const opts = doughnutSdkOptions(signal)
  const contest = await runDefaultBackendJson<QuestionContestResult>(() =>
    RecallPromptController.contest({
      path: { recallPrompt: recallPromptId },
      ...opts,
    })
  )
  if (contest.rejected) {
    return {
      ok: false,
      message: contest.advice ?? 'Question could not be regenerated',
    }
  }
  const regenerated = await runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.regenerate({
      path: { recallPrompt: recallPromptId },
      body: contest,
      ...opts,
    })
  )
  if (!isTerminalRecallQuestion(regenerated)) {
    return { ok: false, message: 'Unexpected question type' }
  }
  return { ok: true, result: { type: 'question', prompt: regenerated } }
}

export const recallCommandDocs = [
  {
    name: '/recall',
    usage: '/recall',
    description: 'Recall all due notes in a session',
    category: 'interactive' as const,
  },
  {
    name: '/recall-status',
    usage: '/recall-status',
    description: 'Show how many notes to recall today',
    category: 'interactive' as const,
  },
]
