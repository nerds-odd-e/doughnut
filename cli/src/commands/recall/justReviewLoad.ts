import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { dueRecallQuery } from './dueRecallQuery.js'

export type RecallJustReviewCardPayload = {
  readonly kind: 'just-review'
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
}

export type RecallMcqCardPayload = {
  readonly kind: 'mcq'
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookTitle?: string
}

export type RecallSessionPayload =
  | RecallJustReviewCardPayload
  | RecallMcqCardPayload

function firstPendingMcq(prompts: RecallPrompt[]): RecallPrompt | undefined {
  return prompts.find((p) => p.questionType === 'MCQ' && p.answer == null)
}

function mcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  prompt: RecallPrompt
): RecallMcqCardPayload | null {
  if (prompt.questionType !== 'MCQ' || prompt.answer != null) return null
  const mq = prompt.multipleChoicesQuestion
  const choices = mq?.f1__choices
  if (choices === undefined || choices.length === 0) return null
  return {
    kind: 'mcq',
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: mq?.f0__stem?.trim() ?? '',
    choices,
    notebookTitle,
  }
}

/** Next due recall card (just-review or MCQ), or `null` when nothing is due in that window. */
export async function loadRecallJustReviewPayloadIfAny(
  dueInDays = 0
): Promise<RecallSessionPayload | null> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueInDays),
      ...doughnutSdkOptions(),
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
      ...doughnutSdkOptions(),
    })
  )
  if (mt.spelling) {
    throw new Error('Spelling recall is not available in the CLI yet.')
  }
  const prompts = await runDefaultBackendJson<RecallPrompt[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(),
    })
  )
  const note = mt.note
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()

  let mcqPrompt = firstPendingMcq(prompts)
  if (mcqPrompt === undefined) {
    try {
      const asked = await runDefaultBackendJson<RecallPrompt>(() =>
        MemoryTrackerController.askAQuestion({
          path: { memoryTracker: id },
          ...doughnutSdkOptions(),
        })
      )
      if (asked.questionType === 'MCQ' && asked.answer == null) {
        mcqPrompt = asked
      }
    } catch {
      // No quiz (e.g. OpenAI off): same as web Quiz.vue → JustReview.
    }
  }

  if (mcqPrompt !== undefined) {
    const mcqPayload = mcqPayloadFromRecallPrompt(id, notebookTitle, mcqPrompt)
    if (mcqPayload !== null) return mcqPayload
  }

  const noteTitle = note?.noteTopology?.title?.trim() || 'Note'
  const detailsMarkdown = (note?.details ?? '').trim()
  return {
    kind: 'just-review',
    memoryTrackerId: id,
    noteTitle,
    detailsMarkdown,
    notebookTitle,
  }
}

export async function markJustReviewRecalled(
  memoryTrackerId: number,
  successful: boolean
): Promise<void> {
  await runDefaultBackendJson(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
      ...doughnutSdkOptions(),
    })
  )
}

export async function submitMcqAnswer(
  recallPromptId: number,
  choiceIndex: number
): Promise<RecallPrompt> {
  return runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex },
      ...doughnutSdkOptions(),
    })
  )
}
