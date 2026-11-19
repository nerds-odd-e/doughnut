import {
  MemoryTrackerController,
  RecallsController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type MemoryTrackerLite,
  type RecallPrompt,
} from 'donut-api'
import {
  donutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/donutBackendClient.js'
import { dueRecallQuery } from './dueRecallQuery.js'
import { noteBreadcrumbTrailTitles } from './recallNoteContext.js'

export async function fetchDueMemoryTrackerIds(
  dueInDays: number,
  signal?: AbortSignal
): Promise<MemoryTrackerLite[]> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueInDays),
      ...donutSdkOptions(signal),
    })
  )
  return due.toRepeat ?? []
}

export type RecallMcqCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookName: string
}

export function recallMcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
  prompt: RecallPrompt
): RecallMcqCardPayload | null {
  const choices = prompt.mcq?.responseChoices
  if (!choices?.length) return null
  return {
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: prompt.mcq?.questionStem?.trim() ?? '',
    choices,
    notebookName: prompt.notebook.name.trim(),
  }
}

/** Just-review recall card (fallback when MCQ is not available). */
export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly contentMarkdown: string
  readonly breadcrumbTitles: readonly string[]
}

function recallJustReviewPayloadFromMemoryTracker(
  mt: MemoryTracker
): RecallJustReviewPayload {
  const note = mt.note
  const topo = note?.noteTopology
  const noteTitle = topo?.title?.trim() || 'Note'
  const contentMarkdown = (note?.content ?? '').trim()
  return {
    memoryTrackerId: mt.id,
    noteTitle,
    contentMarkdown,
    breadcrumbTitles: noteBreadcrumbTrailTitles(
      note,
      mt.recalledNote?.ancestorFolders,
      undefined
    ),
  }
}

/** Spelling memory tracker: server spelling question first (same order as web recall). */
export type SpellingRecallSessionPayload = {
  readonly memoryTrackerId: number
  readonly notebookName?: string
  /** Cached from tracker load when available; answered scrollback prefers `note` on the submit response. */
  readonly contentMarkdown: string
  /** When set, `getRecallPrompt` was already done (e.g. in loadRecallCardForMemoryTrackerId). */
  readonly recallPromptId?: number
  readonly stemMarkdown?: string
}

function spellingRecallSessionPayload(
  memoryTrackerId: number,
  prompt: RecallPrompt | undefined
): SpellingRecallSessionPayload {
  const payload = { memoryTrackerId, contentMarkdown: '' }
  if (!prompt?.spellingQuestion) return payload
  const { spellingQuestion } = prompt
  const notebookName = spellingQuestion.notebook?.name ?? prompt.notebook?.name
  return {
    ...payload,
    notebookName: notebookName?.trim() || undefined,
    recallPromptId: prompt.id,
    stemMarkdown: spellingQuestion.stem ?? '',
  }
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
  const prompt = await runDefaultBackendJson<RecallPrompt>(() =>
    MemoryTrackerController.getRecallPrompt({
      path: { memoryTracker: memoryTrackerId },
      ...donutSdkOptions(signal),
    })
  ).catch(() => undefined)

  if (spelling) {
    return {
      variant: 'spelling-session',
      payload: spellingRecallSessionPayload(memoryTrackerId, prompt),
    }
  }

  const mcq =
    prompt && recallMcqPayloadFromRecallPrompt(memoryTrackerId, prompt)
  if (mcq) return { variant: 'mcq', payload: mcq }

  const mt = await runDefaultBackendJson<MemoryTracker>(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: memoryTrackerId },
      ...donutSdkOptions(signal),
    })
  )
  return {
    variant: 'just-review',
    payload: recallJustReviewPayloadFromMemoryTracker(mt),
  }
}
