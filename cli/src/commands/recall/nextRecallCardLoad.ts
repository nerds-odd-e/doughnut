import {
  MemoryTrackerController,
  RecallsController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { dueRecallQuery } from './dueRecallQuery.js'

/** Just-review recall card (fallback when MCQ is not available). */
export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
}

function recallJustReviewPayloadFromMemoryTracker(
  mt: MemoryTracker
): RecallJustReviewPayload {
  const note = mt.note
  const noteTitle = note?.noteTopology?.title?.trim() || 'Note'
  const detailsMarkdown = (note?.details ?? '').trim()
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()
  return {
    memoryTrackerId: mt.id,
    noteTitle,
    detailsMarkdown,
    notebookTitle,
  }
}

export type RecallMcqCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookTitle?: string
}

function firstPendingMcq(prompts: RecallPrompt[]): RecallPrompt | undefined {
  return prompts.find((p) => p.questionType === 'MCQ' && p.answer == null)
}

export function recallMcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  prompt: RecallPrompt
): RecallMcqCardPayload | null {
  if (prompt.questionType !== 'MCQ' || prompt.answer != null) return null
  const mq = prompt.multipleChoicesQuestion
  const choices = mq?.f1__choices
  if (choices === undefined || choices.length === 0) return null
  return {
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: mq?.f0__stem?.trim() ?? '',
    choices,
    notebookTitle,
  }
}

/**
 * If this due memory tracker has a pending MCQ (existing or from askAQuestion), return it;
 * otherwise null so the session can show just-review instead.
 */
async function tryLoadMcqPayload(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  existingPrompts: RecallPrompt[],
  signal?: AbortSignal
): Promise<RecallMcqCardPayload | null> {
  let mcqPrompt = firstPendingMcq(existingPrompts)
  if (mcqPrompt === undefined) {
    try {
      const asked = await runDefaultBackendJson<RecallPrompt>(() =>
        MemoryTrackerController.askAQuestion({
          path: { memoryTracker: memoryTrackerId },
          ...doughnutSdkOptions(signal),
        })
      )
      if (asked.questionType === 'MCQ' && asked.answer == null) {
        mcqPrompt = asked
      }
    } catch {
      // No quiz (e.g. OpenAI off): same as web Quiz.vue → just-review path.
    }
  }
  if (mcqPrompt === undefined) return null
  return recallMcqPayloadFromRecallPrompt(
    memoryTrackerId,
    notebookTitle,
    mcqPrompt
  )
}

/** Spelling memory tracker: server spelling question first (same order as web recall). */
export type SpellingRecallSessionPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly notebookTitle?: string
}

export type RecallCard =
  | {
      readonly variant: 'just-review'
      readonly payload: RecallJustReviewPayload
    }
  | { readonly variant: 'mcq'; readonly payload: RecallMcqCardPayload }
  | {
      readonly variant: 'spelling-session'
      readonly payload: SpellingRecallSessionPayload
    }

/** Next due recall card (MCQ, just-review fallback, or spelling session), or `null` when nothing is due. */
export async function loadNextRecallCardIfAny(
  dueInDays = 0,
  signal?: AbortSignal
): Promise<RecallCard | null> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueInDays),
      ...doughnutSdkOptions(signal),
    })
  )
  const trackers = due.toRepeat ?? []
  if (trackers.length === 0) return null
  const first = trackers[0]!
  const id = first.memoryTrackerId
  if (id === undefined) return null

  const mt = await runDefaultBackendJson<MemoryTracker>(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(signal),
    })
  )
  const note = mt.note
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()

  if (mt.spelling) {
    const noteTitle = note?.noteTopology?.title?.trim() || 'Note'
    return {
      variant: 'spelling-session',
      payload: {
        memoryTrackerId: mt.id,
        noteTitle,
        notebookTitle,
      },
    }
  }

  const prompts = await runDefaultBackendJson<RecallPrompt[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(signal),
    })
  )

  const mcqPayload = await tryLoadMcqPayload(id, notebookTitle, prompts, signal)
  if (mcqPayload !== null) {
    return { variant: 'mcq', payload: mcqPayload }
  }

  return {
    variant: 'just-review',
    payload: recallJustReviewPayloadFromMemoryTracker(mt),
  }
}
