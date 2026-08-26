import {
  MemoryTrackerController,
  type RecallPrompt,
  type RecallPromptHistoryItem,
} from 'donut-api'
import {
  donutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/donutBackendClient.js'

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

function recallMcqPayload(
  memoryTrackerId: number,
  recallPromptId: number,
  mcq: RecallPrompt['mcq'],
  notebookName: string
): RecallMcqCardPayload | null {
  const choices = mcq?.responseChoices
  if (choices === undefined || choices.length === 0) return null
  return {
    memoryTrackerId,
    recallPromptId,
    stem: mcq?.questionStem?.trim() ?? '',
    choices,
    notebookName: notebookName.trim(),
  }
}

export function recallMcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
  prompt: RecallPrompt
): RecallMcqCardPayload | null {
  return recallMcqPayload(
    memoryTrackerId,
    prompt.id,
    prompt.mcq,
    prompt.notebook.name
  )
}

function recallMcqPayloadFromRecallPromptHistoryItem(
  memoryTrackerId: number,
  prompt: RecallPromptHistoryItem,
  notebookName: string
): RecallMcqCardPayload | null {
  if (prompt.questionType !== 'MCQ' || prompt.answer != null) return null
  return recallMcqPayload(memoryTrackerId, prompt.id, prompt.mcq, notebookName)
}

/**
 * If this due memory tracker has a pending MCQ (existing or from getRecallPrompt), return it;
 * otherwise null so the session can show just-review instead.
 */
export async function tryLoadMcqPayload(
  memoryTrackerId: number,
  existingPrompts: RecallPromptHistoryItem[],
  notebookName: string,
  signal?: AbortSignal
): Promise<RecallMcqCardPayload | null> {
  const mcqPrompt = firstPendingMcq(existingPrompts)
  if (mcqPrompt === undefined) {
    try {
      const prompt = await runDefaultBackendJson<RecallPrompt>(() =>
        MemoryTrackerController.getRecallPrompt({
          path: { memoryTracker: memoryTrackerId },
          ...donutSdkOptions(signal),
        })
      )
      if (prompt.mcq != null) {
        const mapped = recallMcqPayloadFromRecallPrompt(memoryTrackerId, prompt)
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
