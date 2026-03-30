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
import {
  recallJustReviewPayloadFromMemoryTracker,
  type RecallJustReviewPayload,
} from './justReviewLoad.js'
import {
  tryLoadMcqPayload,
  type RecallMcqCardPayload,
} from './recallMcqLoad.js'
import type { SpellingRecallSessionPayload } from './recallSpellingLoad.js'

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
  const prompts = await runDefaultBackendJson<RecallPrompt[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(signal),
    })
  )
  const note = mt.note
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()

  if (mt.spelling) {
    const jr = recallJustReviewPayloadFromMemoryTracker(mt)
    return {
      variant: 'spelling-session',
      payload: {
        memoryTrackerId: jr.memoryTrackerId,
        noteTitle: jr.noteTitle,
        detailsMarkdown: jr.detailsMarkdown,
        notebookTitle: jr.notebookTitle,
      },
    }
  }

  const mcqPayload = await tryLoadMcqPayload(id, notebookTitle, prompts, signal)
  if (mcqPayload !== null) {
    return { variant: 'mcq', payload: mcqPayload }
  }

  return {
    variant: 'just-review',
    payload: recallJustReviewPayloadFromMemoryTracker(mt),
  }
}
