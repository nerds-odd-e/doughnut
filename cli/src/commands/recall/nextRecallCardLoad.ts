import {
  MemoryTrackerController,
  RecallsController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type MemoryTrackerLite,
  type RecallPromptHistoryItem,
  type RecallQuestion,
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
      mt.ancestorFolders,
      undefined
    ),
  }
}

export type RecallMcqCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookName: string
}

function firstPendingMcq(
  prompts: RecallPromptHistoryItem[]
): RecallPromptHistoryItem | undefined {
  return prompts.find((p) => p.questionType === 'MCQ' && p.answer == null)
}

export function recallMcqPayloadFromRecallQuestion(
  memoryTrackerId: number,
  prompt: RecallQuestion
): RecallMcqCardPayload | null {
  const mq = prompt.multipleChoicesQuestion
  const choices = mq?.responseChoices
  if (choices === undefined || choices.length === 0) return null
  return {
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: mq?.questionStem?.trim() ?? '',
    choices,
    notebookName: prompt.notebook.name.trim(),
  }
}

export function recallMcqPayloadFromRecallPromptHistoryItem(
  memoryTrackerId: number,
  prompt: RecallPromptHistoryItem,
  notebookName: string
): RecallMcqCardPayload | null {
  if (prompt.questionType !== 'MCQ' || prompt.answer != null) return null
  const mq = prompt.multipleChoicesQuestion
  const choices = mq?.responseChoices
  if (choices === undefined || choices.length === 0) return null
  return {
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: mq?.questionStem?.trim() ?? '',
    choices,
    notebookName: notebookName.trim(),
  }
}

/**
 * If this due memory tracker has a pending MCQ (existing or from askAQuestion), return it;
 * otherwise null so the session can show just-review instead.
 */
async function tryLoadMcqPayload(
  memoryTrackerId: number,
  existingPrompts: RecallPromptHistoryItem[],
  notebookName: string,
  signal?: AbortSignal
): Promise<RecallMcqCardPayload | null> {
  const mcqPrompt = firstPendingMcq(existingPrompts)
  if (mcqPrompt === undefined) {
    try {
      const asked = await runDefaultBackendJson<RecallQuestion>(() =>
        MemoryTrackerController.askAQuestion({
          path: { memoryTracker: memoryTrackerId },
          ...doughnutSdkOptions(signal),
        })
      )
      if (asked.multipleChoicesQuestion != null) {
        const mapped = recallMcqPayloadFromRecallQuestion(
          memoryTrackerId,
          asked
        )
        if (mapped !== null) {
          return mapped
        }
      }
    } catch {
      // No quiz (e.g. OpenAI off): same as web Quiz.vue → just-review path.
    }
  }
  if (mcqPrompt === undefined) return null
  return recallMcqPayloadFromRecallPromptHistoryItem(
    memoryTrackerId,
    mcqPrompt,
    notebookName
  )
}

/** Spelling memory tracker: server spelling question first (same order as web recall). */
export type SpellingRecallSessionPayload = {
  readonly memoryTrackerId: number
  readonly notebookName?: string
  /** Cached from tracker load when available; answered scrollback prefers `note` on the submit response. */
  readonly contentMarkdown: string
  /** When set, `askAQuestion` was already done (e.g. in loadRecallCardForMemoryTrackerId). */
  readonly recallPromptId?: number
  readonly stemMarkdown?: string
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
    try {
      const prompt = await runDefaultBackendJson<RecallQuestion>(() =>
        MemoryTrackerController.askAQuestion({
          path: { memoryTracker: memoryTrackerId },
          ...doughnutSdkOptions(signal),
        })
      )
      if (prompt.spellingQuestion != null) {
        const fromPrompt =
          prompt.spellingQuestion.notebook?.name ?? prompt.notebook?.name
        const notebookName = fromPrompt?.trim()
        return {
          variant: 'spelling-session',
          payload: {
            memoryTrackerId,
            notebookName:
              notebookName !== undefined && notebookName.length > 0
                ? notebookName
                : undefined,
            contentMarkdown: '',
            recallPromptId: prompt.id,
            stemMarkdown: prompt.spellingQuestion.stem ?? '',
          },
        }
      }
    } catch {
      // Same as web when askAQuestion fails: stage will retry.
    }
    return {
      variant: 'spelling-session',
      payload: {
        memoryTrackerId,
        contentMarkdown: '',
      },
    }
  }

  const prompts = await runDefaultBackendJson<RecallPromptHistoryItem[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )

  const mcqPayload = await tryLoadMcqPayload(
    memoryTrackerId,
    prompts,
    '',
    signal
  )
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
