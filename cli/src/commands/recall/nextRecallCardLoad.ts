import {
  MemoryTrackerController,
  RecallsController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type MemoryTrackerLite,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { dueRecallQuery } from './dueRecallQuery.js'
import { noteBreadcrumbTrailTitles } from './recallNoteContext.js'

function shuffleMemoryTrackerLites(
  lites: readonly MemoryTrackerLite[]
): MemoryTrackerLite[] {
  const a = [...lites]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const t = a[i]!
    a[i] = a[j]!
    a[j] = t
  }
  return a
}

export async function fetchDueMemoryTrackerIds(
  dueInDays: number,
  signal?: AbortSignal
): Promise<MemoryTrackerLite[]> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueInDays),
      ...doughnutSdkOptions(signal),
    })
  )
  return due.toRepeat ?? []
}

export async function fetchShuffledDueMemoryTrackerIds(
  dueInDays: number,
  signal?: AbortSignal
): Promise<MemoryTrackerLite[]> {
  return shuffleMemoryTrackerLites(
    await fetchDueMemoryTrackerIds(dueInDays, signal)
  )
}

/** Just-review recall card (fallback when MCQ is not available). */
export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
  readonly breadcrumbTitles: readonly string[]
}

function recallJustReviewPayloadFromMemoryTracker(
  mt: MemoryTracker
): RecallJustReviewPayload {
  const note = mt.note
  const topo = note?.noteTopology
  const noteTitle = topo?.title?.trim() || 'Note'
  const detailsMarkdown = (note?.details ?? '').trim()
  const notebookTitle = topo?.notebookTitle?.trim()
  return {
    memoryTrackerId: mt.id,
    noteTitle,
    detailsMarkdown,
    notebookTitle,
    breadcrumbTitles: noteBreadcrumbTrailTitles(note),
  }
}

export type RecallMcqCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookTitle: string
}

function firstPendingMcq(prompts: RecallPrompt[]): RecallPrompt | undefined {
  return prompts.find((p) => p.questionType === 'MCQ' && p.answer == null)
}

export function recallMcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
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
    notebookTitle: prompt.notebook.title.trim(),
  }
}

/**
 * If this due memory tracker has a pending MCQ (existing or from askAQuestion), return it;
 * otherwise null so the session can show just-review instead.
 */
async function tryLoadMcqPayload(
  memoryTrackerId: number,
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
  return recallMcqPayloadFromRecallPrompt(memoryTrackerId, mcqPrompt)
}

/** Spelling memory tracker: server spelling question first (same order as web recall). */
export type SpellingRecallSessionPayload = {
  readonly memoryTrackerId: number
  readonly notebookTitle?: string
  /** Cached from tracker load when available; answered scrollback prefers `note` on the submit response. */
  readonly detailsMarkdown: string
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

/** Build one recall card for a known due memory tracker (no `recalling` list fetch). */
export async function loadRecallCardForMemoryTrackerId(
  memoryTrackerId: number,
  spelling: boolean,
  signal?: AbortSignal
): Promise<RecallCard> {
  if (spelling) {
    return {
      variant: 'spelling-session',
      payload: {
        memoryTrackerId,
        notebookTitle: undefined,
        detailsMarkdown: '',
      },
    }
  }

  const prompts = await runDefaultBackendJson<RecallPrompt[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )

  const mcqPayload = await tryLoadMcqPayload(memoryTrackerId, prompts, signal)
  if (mcqPayload !== null) {
    return { variant: 'mcq', payload: mcqPayload }
  }

  const mt = await runDefaultBackendJson<MemoryTracker>(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )
  return {
    variant: 'just-review',
    payload: recallJustReviewPayloadFromMemoryTracker(mt),
  }
}
