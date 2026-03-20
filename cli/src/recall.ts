import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  type MemoryTrackerLite,
  type RecallPrompt,
} from 'doughnut-api'
import { runWithDefaultBackendClient } from './accessToken.js'
import { userAbortError } from './fetchAbort.js'

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

function getTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

function backendSdkOpts(signal?: AbortSignal) {
  return { throwOnError: true as const, ...(signal ? { signal } : {}) }
}

export async function recallStatus(signal?: AbortSignal): Promise<string> {
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays: 0 },
      ...backendSdkOpts(signal),
    })
  )
  const count = result.data?.toRepeat?.length ?? 0
  if (count === 1) {
    return '1 note to recall today'
  }
  return `${count} notes to recall today`
}

export type RecallNextResult =
  | { type: 'none'; message: string }
  | {
      type: 'just-review'
      memoryTrackerId: number
      title: string
      details?: string
    }
  | {
      type: 'mcq'
      recallPromptId: number
      stem: string
      choices: string[]
    }
  | {
      type: 'spelling'
      recallPromptId: number
      stem: string
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
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays },
      ...backendSdkOpts(abortSignal),
    })
  )
  const toRepeat = result.data?.toRepeat ?? []
  const first = toRepeat[0] as MemoryTrackerLite | undefined
  if (!first?.memoryTrackerId) {
    return { type: 'none', message: '0 notes to recall today' }
  }

  const questionResult = await runWithDefaultBackendClient(() =>
    MemoryTrackerController.askAQuestion({
      path: { memoryTracker: first.memoryTrackerId },
      ...backendSdkOpts(abortSignal),
    })
  )
  const prompt = questionResult.data
  if (prompt?.questionType === 'MCQ' && prompt.multipleChoicesQuestion) {
    const mcq = prompt.multipleChoicesQuestion
    const stem = mcq.f0__stem ?? ''
    const choices = mcq.f1__choices ?? []
    return {
      type: 'mcq',
      recallPromptId: prompt.id,
      stem,
      choices,
    }
  }
  if (prompt?.questionType === 'SPELLING') {
    const stem = prompt.spellingQuestion?.stem ?? ''
    return {
      type: 'spelling',
      recallPromptId: prompt.id,
      stem,
    }
  }

  const trackerResult = await runWithDefaultBackendClient(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: first.memoryTrackerId },
      ...backendSdkOpts(abortSignal),
    })
  )
  const title = trackerResult.data?.note?.noteTopology?.title ?? 'Untitled note'
  const details = trackerResult.data?.note?.details
  return {
    type: 'just-review',
    memoryTrackerId: first.memoryTrackerId,
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
      ...backendSdkOpts(),
    })
  )
}

export async function answerQuiz(
  recallPromptId: number,
  choiceIndex: number,
  thinkingTimeMs?: number
): Promise<{ correct: boolean }> {
  const result = await runWithDefaultBackendClient(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex, ...(thinkingTimeMs != null && { thinkingTimeMs }) },
      ...backendSdkOpts(),
    })
  )
  const correct = result.data?.answer?.correct ?? false
  return { correct }
}

export async function answerSpelling(
  recallPromptId: number,
  spellingAnswer: string,
  thinkingTimeMs?: number
): Promise<{ correct: boolean }> {
  const result = await runWithDefaultBackendClient(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body: {
        spellingAnswer,
        ...(thinkingTimeMs != null && { thinkingTimeMs }),
      },
      ...backendSdkOpts(),
    })
  )
  const correct = result.data?.answer?.correct ?? false
  return { correct }
}

function recallPromptToResult(prompt: RecallPrompt): RecallNextResult | null {
  if (prompt.questionType === 'MCQ' && prompt.multipleChoicesQuestion) {
    const mcq = prompt.multipleChoicesQuestion
    return {
      type: 'mcq',
      recallPromptId: prompt.id,
      stem: mcq.f0__stem ?? '',
      choices: mcq.f1__choices ?? [],
    }
  }
  if (prompt.questionType === 'SPELLING') {
    const stem = prompt.spellingQuestion?.stem ?? ''
    return { type: 'spelling', recallPromptId: prompt.id, stem }
  }
  return null
}

export async function contestAndRegenerate(
  recallPromptId: number,
  signal?: AbortSignal
): Promise<
  { ok: true; result: RecallNextResult } | { ok: false; message: string }
> {
  const contestResult = await runWithDefaultBackendClient(() =>
    RecallPromptController.contest({
      path: { recallPrompt: recallPromptId },
      ...backendSdkOpts(signal),
    })
  )
  const data = contestResult.data
  if (!data) {
    return { ok: false, message: 'Contest failed' }
  }
  if (data.rejected) {
    return {
      ok: false,
      message: data.advice ?? 'Question could not be regenerated',
    }
  }
  const regenerateResult = await runWithDefaultBackendClient(() =>
    RecallPromptController.regenerate({
      path: { recallPrompt: recallPromptId },
      body: data,
      ...backendSdkOpts(signal),
    })
  )
  const regenerated = regenerateResult.data
  if (!regenerated) {
    return { ok: false, message: 'Regenerate failed' }
  }
  const result = recallPromptToResult(regenerated)
  if (!result) {
    return { ok: false, message: 'Unexpected question type' }
  }
  return { ok: true, result }
}

export const recallCommandDocs = [
  {
    name: '/recall',
    usage: '/recall',
    description: 'Recall all due notes in a session',
    category: 'interactive' as const,
    interactiveOnly: true,
  },
  {
    name: '/recall-status',
    usage: '/recall-status',
    description: 'Show how many notes to recall today',
    category: 'interactive' as const,
  },
]
